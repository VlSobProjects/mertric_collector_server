package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class MetricCollectorGroup<P,S extends MetricConfig,R extends DataResponse<S> & ResponseWithMessage<R>,W extends MetricWriter<P,?,?,?>,C extends MetricCollector<P,?,?,?>> extends MetricCollector<P,S,W,R> {

    @Getter
    private final Set<C> collectors = new HashSet<>();

    protected MetricCollectorGroup(S config, W dbConnector) {
        super(config, dbConnector);
    }


    public Mono<R> addMetricCollectorIfValidateMono(C collector){
        return addMetricCollectorIfValidate(collector,collector.validate());
    }

    public Mono<R> addMetricCollectorIfValidateMono(C collector,JsonNode data){
        return addMetricCollectorIfValidate(collector,collector.validateData(data));
    }

    private Mono<R> addMetricCollectorIfValidate(C collector,Mono<List<JsonNode>> validationResultMono){
        return validationResultMono.map(l->{
            if(collector.isValidationFail(l)){
                R response = response().setMessage("Fail to add Collector - collector validation fail");
                response.dataArray(collector,l);
                return response;
            } else {
                if(collectors.add(collector)) return response().setMessage("Collector validate and added");
                else {
                    R response = response().setMessage("Collector is duplicated");
                    response.dataArray(collector,collectors.stream().filter(c->c.equals(collector)).findFirst().orElse(null));
                    return response;
                }
            }
        });
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
