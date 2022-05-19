package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.MetricCollector;
import ru.iteco.nt.metric_collector_server.MetricWriter;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;


@Slf4j
public class InfluxMetricCollector extends MetricCollector<
        Point
        ,InfluxMetricCollectorConfig
        ,MetricWriter<Point,?,?,?>
        ,InfluxMetricCollectorResponse
        > {


    public InfluxMetricCollector(InfluxMetricCollectorConfig config){
        super(config);
    }

    public InfluxMetricCollector(InfluxMetricCollectorConfig config,MetricWriter<Point,?,?,?> dbConnector){
        super(config, dbConnector);
    }

    @Override
    public InfluxMetricCollectorResponse response(){
        return InfluxMetricCollectorResponse.builder()
                .collecting(isRunning())
                .dbConnection(getDbConnector()==null ? null : getDbConnector().response())
                .time(System.currentTimeMillis())
                .settings(getConfig().shortVersion())
                .id(getId())
                .build();
    }

    @Override
    public boolean isValidationFail(List<JsonNode> validationResult) {
        return validationResult.stream().anyMatch(j->j.has("error"));
    }

    public void addPointFromData(JsonNode data,List<Point> list,Instant time){
        Supplier<Point.Builder> builderSupplier = getConfig().isSetTime()?
                ()->Point.measurement(getConfig().getMeasurement()).time(time.toEpochMilli(),TimeUnit.MILLISECONDS) :
                ()->Point.measurement(getConfig().getMeasurement());

        List<Function<Point.Builder,Point.Builder>> setters = getConfig().getFields()
                .stream()
                .map(InfluxFieldsCollector::new)
                .map(f->f.getPointSetters(data))
                .filter(l->l.size()>0)
                .reduce((l1,l2)->Utils.reduceSetters(l1,l2,Utils.toArrayNode(getConfig().getFields().stream().map(InfluxField::shortVersion).collect(Collectors.toList()))))
                .orElse(new ArrayList<>());
        setters.forEach(s-> {
            Point.Builder builder = s.apply(builderSupplier.get());
            if(builder.hasFields())
                list.add(builder.build());
        });
    }

    public Mono<List<JsonNode>> validate(){
        return Flux.fromIterable(getConfig().getFields())
                .map(InfluxFieldsCollector::new)
                .flatMap(InfluxFieldsCollector::validate)
                .collectList();
    }
    public Mono<List<JsonNode>> validateData(JsonNode data){
        return Flux.fromIterable(getConfig().getFields())
                .map(InfluxFieldsCollector::new)
                .flatMap(c->c.validateData(data))
                .collectList();
    }


}
