package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MetricCollectorGroup<P,S extends MetricConfig,R extends DataResponse<S> & ResponseWithMessage<R>,W extends MetricWriter<P,?,?,?>,C extends MetricCollector<P,?,?,?>> extends MetricCollector<P,S,W,R> {

    @Getter
    private final Set<C> collectors = new HashSet<>();
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
    }

    @Override
    public Mono<List<JsonNode>> validate() {
        return Flux.concat(collectors
                .stream()
                .map(MetricCollector::validate)
                .collect(Collectors.toList())
        ).reduce(new ArrayList<>(),(l1, l2)-> {
                    l1.addAll(l2);
                    return l1;
                }
        );
    }

    @Override
    public <R1 extends DataResponse<?> & ResponseWithMessage<R1>> Mono<R1> validateAndSet(Mono<R1> response, Mono<JsonNode> data, UnaryOperator<Mono<R1>> addIfNotFail) {
        return Flux.concat(collectors
                .stream()
                .flatMap(c-> Stream.of(c.validate(),data.flatMap(c::validateData)))
                .collect(Collectors.toList())
        ).reduce(new ArrayList<JsonNode>(),(l1,l2)->{
            l1.addAll(leaveOnlyAndSetErrorsData(l2));
            return l1;
        }).flatMap(l->l.size()==0 ? Utils.setMessageAndData(response,"No error Found") : Utils.setData(response,Utils.getError(getClass().getSimpleName(),"Validation Fail",l)));
    }

    @Override
    public Mono<List<JsonNode>> validateData(JsonNode data) {
        return Flux.concat(collectors
                .stream()
                .map(c->c.validateData(data))
                .collect(Collectors.toList())
        ).reduce(new ArrayList<>(),(l1,l2)-> {
                    l1.addAll(l2);
                    return l1;
                }
        );
    }

    @Override
    public boolean isValidationFail(List<JsonNode> validationResult) {
        return collectors.stream().findFirst().map(c->c.isValidationFail(validationResult)).orElse(false);
    }
}
