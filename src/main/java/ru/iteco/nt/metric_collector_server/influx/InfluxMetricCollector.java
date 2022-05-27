package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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


    public InfluxMetricCollector(InfluxMetricCollectorConfig config,int id){
        super(config,id);
    }

    public InfluxMetricCollector(InfluxMetricCollectorConfig config,MetricWriter<Point,?,?,?> dbConnector){
        super(config, dbConnector);
    }

    @Override
    public InfluxMetricCollectorResponse response(){
        return InfluxMetricCollectorResponse.builder()
                .collecting(isRunning())
                .writer(getDbConnector()==null ? null : getDbConnector().response())
                .time(System.currentTimeMillis())
                .settings(getConfig().shortVersion())
                .validateData(isDataValidated())
                .validateDataPass(isValidateDataPass())
                .id(getId())
                .build();
    }

    @Override
    public boolean isValidationFail(List<JsonNode> validationResult) {
        return validationResult.stream().anyMatch(j->j.has("error"));
    }

    public void addPointFromData(JsonNode data,List<Point> list,Instant time){
        JsonNode source = getConfig().hasPath() ? Utils.getFromJsonNode(data,getConfig().getPath()) : data;
        addPointFromJsonNode(source,list,time);
    }

    private void addPointFromJsonNode(JsonNode source,List<Point> list,Instant time){
        if(source instanceof ObjectNode)
            addPointFromDataObject((ObjectNode) source,list,time);
        else if(source instanceof ArrayNode)
            source.forEach(j->addPointFromJsonNode(j,list,time));
        else log.error("collector source is not Object: {}",source);
    }

    private void addPointFromDataObject(ObjectNode data,List<Point> list,Instant time){
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

    public Mono<List<JsonNode>> validateMono(){
        return Flux.fromIterable(getConfig().getFields())
                .map(InfluxFieldsCollector::new)
                .flatMap(InfluxFieldsCollector::validate)
                .collectList();
    }

    public List<JsonNode> validateData(JsonNode data){
        return validateDataSource(getConfig().hasPath() ? Utils.getFromJsonNode(data,getConfig().getPath()) : data,new ArrayList<>());
    }

    private List<JsonNode> validateDataSource(JsonNode data,List<JsonNode> result){
        if(data instanceof ArrayNode){
            data.forEach(j->validateDataSource(j, result));
        } else result.addAll(
                getConfig()
                        .getFields()
                        .stream()
                        .map(InfluxFieldsCollector::new)
                        .map(c->c.validateData(data))
                        .collect(Collectors.toList())
        );
        return result;
    }


}
