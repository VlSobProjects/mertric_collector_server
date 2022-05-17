package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class MetricCollectorGroup<P,S,R extends DataResponse<S> & ResponseWithMessage<R>,W extends MetricWriter<P,?,?,?>,C extends MetricCollector<P,?,?,?>> extends MetricCollector<P,S,W,R> {

    @Getter
    private final List<C> collectors = new CopyOnWriteArrayList<>();

    protected MetricCollectorGroup(S config, W dbConnector) {
        super(config, dbConnector);
    }

    public R addMetricCollector(C collector){
        collectors.add(collector);
        return response();
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
}
