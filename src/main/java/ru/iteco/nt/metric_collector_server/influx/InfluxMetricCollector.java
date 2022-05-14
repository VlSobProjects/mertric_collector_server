package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;


@Slf4j
public class InfluxMetricCollector {

    private final static AtomicInteger ID_SOURCE = new AtomicInteger();

    private final int id;
    private final InfluxMetricCollectorConfig config;

    public InfluxMetricCollector(InfluxMetricCollectorConfig config){
        this.config = config;
        id = ID_SOURCE.incrementAndGet();
    }


    public void addPointFromData(JsonNode data,List<Point> list,Instant time){
        Supplier<Point.Builder> builderSupplier = config.isSetTime()?
                ()->Point.measurement(config.getMeasurement()).time(time.toEpochMilli(),TimeUnit.MILLISECONDS) :
                ()->Point.measurement(config.getMeasurement());

        List<Function<Point.Builder,Point.Builder>> setters = config.getFields()
                .stream()
                .map(InfluxFieldsCollector::new)
                .map(f->f.getPointSetters(data))
                .filter(l->l.size()>0)
                .reduce((l1,l2)->Utils.reduceSetters(l1,l2,Utils.toArrayNode(config.getFields().stream().map(InfluxField::shortVersion).collect(Collectors.toList()))))
                .orElse(new ArrayList<>());
        setters.forEach(s-> {
            Point.Builder builder = s.apply(builderSupplier.get());
            if(builder.hasFields())
                list.add(builder.build());
        });
    }

    public void addPointFromData(JsonNode data,List<Point> list){
        addPointFromData(data, list,Instant.now());
    }

    public List<Point> getPointFromData(JsonNode data,Instant time){
        List<Point> list = new ArrayList<>();
        addPointFromData(data,list,time);
        return list;
    }

    public List<Point> getPointFromData(JsonNode data){
        return getPointFromData(data,Instant.now());
    }

    public Mono<List<JsonNode>> validate(){
        return Flux.fromIterable(config.getFields())
                .map(InfluxFieldsCollector::new)
                .flatMap(InfluxFieldsCollector::validate)
                .collectList();
    }
    public Mono<List<JsonNode>> validateData(JsonNode data){
        return Flux.fromIterable(config.getFields())
                .map(InfluxFieldsCollector::new)
                .flatMap(c->c.validateData(data))
                .collectList();
    }


}
