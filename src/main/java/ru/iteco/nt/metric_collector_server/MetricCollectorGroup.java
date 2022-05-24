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
        return collector.validateAndSet(responseMono(),collectorHolder.getApiCallHolder().lastApiCall(),r->{
            collectors.add(collector);
            setValidate(collectors.stream().anyMatch(MetricCollector::isValidate));
            log.debug("isValidate {}",isValidate());
            if(isValidate()){
                start(Flux.concat(collectorHolder.getApiCallHolder().lastApiCall(),collectorHolder.getCollector()));
            }
            return Utils.setMessageAndData(r,"Collector added",config);
        });
    }

    public Mono<R> validateAndRemove(Mono<JsonNode> data){
        if(collectors.size()==0) return responseMono();
        AtomicBoolean fail = new AtomicBoolean();
        return collectors.stream().map(c->c.validateAndRemove(responseMono(),data,r->{
            collectors.remove(c);
            fail.set(true);
            return Utils.modifyData(r,j->((ObjectNode)j).set("collector",Utils.valueToTree(c.response())));
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

    @Override
    public boolean isValidationFail(List<JsonNode> validationResult) {
        return collectors.stream().findFirst().map(c->c.isValidationFail(validationResult)).orElse(false);
    }

    @Override
    public void validateDataAndSet(JsonNode data) {
        setValidate(collectors.stream().peek(c->c.validateDataAndSet(data)).anyMatch(MetricCollector::isValidate));

    }
}
