package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiCollectorHolder;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Slf4j
public abstract class MetricCollectorGroup<P,S extends MetricConfig,R extends DataResponse<S> & ResponseWithMessage<R>,W extends MetricWriter<P,?,?,?>,C extends MetricCollector<P,?,?,?>> extends MetricCollector<P,S,W,R> {

    @Getter
    private final Set<C> collectors = new CopyOnWriteArraySet<>();
    private final AtomicInteger collectorId = new AtomicInteger();

    protected MetricCollectorGroup(S config, W dbConnector) {
        super(config, dbConnector);
    }

    public int getCollectorId(){
        return collectorId.getAndIncrement();
    }

    @Override
    public void addPointFromData(JsonNode data, List<P> list, Instant time) {
        collectors.forEach(c->c.addPointFromData(data,list,time));
        log.debug("add point: list size: {}", list.size());
    }

    @Override
    public Mono<List<JsonNode>> validateMono() {
        return Flux.concat(collectors
                .stream()
                .map(MetricCollector::validateMono)
                .collect(Collectors.toList())
        ).reduce(new ArrayList<>(),(l1, l2)-> {
                    l1.addAll(l2);
                    return l1;
                }
        );
    }

    public <SC extends MetricConfig> Mono<R> validateAndAdd(ApiCollectorHolder collectorHolder, SC config, Function<SC,C> create){
        if(collectors.stream().anyMatch(c->c.getConfig().equals(config))) return Utils.setMessageAndData(responseMono(),"Error: Metric Collector with same config exist in group",Utils.getError(getClass().getSimpleName(),"Duplicated config" ,config));
        C collector = create.apply(config);
        return collector.validateAndSet(responseMono(),r->{
            collectors.add(collector);
            log.debug("Collector added {}",config.shortVersion());
            return Utils.setMessageAndData(r,"Collector added",config);
        });
    }

    public Mono<R> validateAndRemove(Mono<JsonNode> data){
        if(collectors.size()==0) return responseMono();
        AtomicBoolean fail = new AtomicBoolean();
        return collectors.stream().map(c->c.validateAndRemove(responseMono(),data,r->{
            collectors.remove(c);
            fail.set(true);
            return Utils.modifyData(r,j->((ObjectNode)j).set("collector",Utils.valueToTree(c.responseWithError())));
        })).reduce(responseMono(),Utils::reduceResponseData).flatMap(r->fail.get() ?
                Utils.setMessageAndData(Mono.just(r),"Removed validation fail metric collectors") :
                        Utils.setMessageAndData(Mono.just(r),"All Collectors pass validation")
                );
    }

    @Override
    protected <R1 extends DataResponse<?> & ResponseWithMessage<R1>> Mono<R1> validateAndDo(Mono<R1> response, Mono<JsonNode> data, boolean doOnError, UnaryOperator<Mono<R1>> doOn) {
        return response.flatMap(r->(r.isFail() &&  doOnError) || (!r.isFail() && !doOnError) ? doOn.apply(Mono.just(r)) : Mono.just(r));
    }


    @Override
    public List<JsonNode> validateData(JsonNode data) {
        return collectors.stream().map(c->c.validateData(data)).reduce(new ArrayList<>(),(l1,l2)-> {
            l1.addAll(l2);
            return l1;
        });
    }

    public Mono<R> getValidateFail() {
        List<DataResponse<?>> list = collectors.stream().filter(VALIDATION_FAIL).map(MetricCollector::responseWithValidationError).collect(Collectors.toList());
        return Utils.setData(responseMono(),list);
    }

    public Mono<R> removeValidateFail(){
        List<DataResponse<?>> list = collectors.stream().filter(VALIDATION_FAIL).peek(collectors::remove).map(MetricCollector::responseWithValidationError).collect(Collectors.toList());
        return Utils.setData(responseMono(),list);
    }

    @Override
    public boolean isValidationFail(List<JsonNode> validationResult) {
        return collectors.stream().findFirst().map(c->c.isValidationFail(validationResult)).orElse(false);
    }

    @Override
    public void validateDataAndSet(JsonNode data) {
        setValidationDataPass(collectors.stream().peek(c->c.validateDataAndSet(data)).anyMatch(MetricCollector::isValidateDataPass));
        setValidationData(collectors.stream().allMatch(MetricCollector::isDataValidated));
    }

    protected String getValidationMessage(){
        String message;
        if(getCollectors().size()==0){
            message = "No collectors.";
        } else {
            long notValidated = getCollectors().stream().filter(m->!m.isDataValidated()).count();
            int collectorSize = getCollectors().size();
            long validateButNotPass = getCollectors().stream().filter(MetricCollector::isDataValidated).filter(m-> !m.isValidateDataPass()).count();
            if(notValidated==collectorSize)
                message = "Metric Collectors is not validated "+notValidated+".";
            else {
                if(notValidated==0){
                    message = "All Collectors validated.";
                } else message = notValidated+" collectors not validated.";
                if(validateButNotPass==0)
                    message+=" All validated collector Pass Validation";
                else message+=" "+validateButNotPass+" fail validation";
            }
        }
        return message;
    }

    @Override
    public R responseWithError() {
        List<DataResponse<?>> list = collectors.stream().filter(VALIDATION_FAIL).map(MetricCollector::responseWithValidationError).collect(Collectors.toList());
        R response = response();
        response.dataArray(list);
        return response();
    }
}
