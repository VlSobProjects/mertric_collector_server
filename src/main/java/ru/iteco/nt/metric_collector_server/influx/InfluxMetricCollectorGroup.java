package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.influxdb.dto.Point;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InfluxMetricCollectorGroup implements InfluxCollector<InfluxMetricCollectorGroupResponse> {

    private final static AtomicInteger ID_SOURCE = new AtomicInteger();
    @Getter
    private final int id;
    private final InfluxMetricCollectorGroupConfig config;
    private final List<InfluxMetricCollector> collectors = new CopyOnWriteArrayList<>();
    private final InfluxDbConnector dbConnector;
    @Setter
    @Getter
    private Disposable disposable;

    public InfluxMetricCollectorGroup(InfluxDbConnector dbConnector,InfluxMetricCollectorGroupConfig config) {
        this.dbConnector = dbConnector;
        this.config=config;
        id = ID_SOURCE.getAndIncrement();
    }

    @Override
    public InfluxMetricCollectorGroupResponse response() {
        return InfluxMetricCollectorGroupResponse.builder()
                .time(System.currentTimeMillis())
                .collecting(isRunning())
                .dbConnection(dbConnector.response())
                .collectors(collectors.stream().map(InfluxMetricCollector::response).collect(Collectors.toList()))
                .id(id)
                .settings(config)
                .build()
                ;
    }

    public InfluxMetricCollectorGroupResponse addInfluxMetricCollector(InfluxMetricCollectorConfig collector){
        collectors.add(new InfluxMetricCollector(collector));
        return response();
    }

    @Override
    public Disposable setUpCollectingDisposable(Flux<JsonNode> source) {
        return source.subscribe(data->{
            if(collectors.size()>0){
                List<Point> list = new ArrayList<>();
                Instant time = Instant.now();
                collectors.forEach(c->c.addPointFromData(data,list,time));
                dbConnector.addPoints(list);
            }
        });
    }


}
