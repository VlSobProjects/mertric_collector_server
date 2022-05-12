package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;
import ru.iteco.nt.metric_collector_server.utils.FieldValueConvertor;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InfluxFieldsCollector {

    private final InfluxField influxField;




    public List<Function<Point.Builder,Point.Builder>> getPointSetters(JsonNode source){
        return getFieldSetters(new AtomicReference<>(b->b),source);
    }

    public List<Function<Point.Builder,Point.Builder>> getFieldSetters(AtomicReference<Function<Point.Builder,Point.Builder>> mainSetter,JsonNode source){
        List<Function<Point.Builder,Point.Builder>> list = new ArrayList<>();
        if(influxField.getValue()!=null && influxField.getValue().isValueNode()){
            mainSetter.set(mainSetter.get().andThen(b->setValueFromNode(b,influxField,influxField.getValue())));
        }
        if(influxField.getPath()!=null && !influxField.getPath().trim().isEmpty()){
            JsonNode node = Utils.getFromJsonNode(source,influxField.getPath());
            if(influxField.getChildren()==null || influxField.getChildren().isEmpty()){
                if(node.isValueNode()){
                    mainSetter.set(mainSetter.get().andThen(b->setValueFromNode(b,influxField,node)));
                } else if(node.isArray()){
                    node.forEach(n->list.add(mainSetter.get().andThen(b->setValueFromNode(b,influxField,n))));
                }
            } else {
                if(node.isArray()){
                    node.forEach(n->
                            list.addAll(influxField.getChildren()
                                    .stream()
                                    .map(InfluxFieldsCollector::new)
                                    .map(c -> c.getFieldSetters(new AtomicReference<>(mainSetter.get()), n))
                                    .reduce(InfluxFieldsCollector::reduceSetters)
                                    .orElse(new ArrayList<>())
                            )
                    );
                } else {
                    list.addAll(influxField.getChildren().stream().map(InfluxFieldsCollector::new)
                            .map(c->c.getFieldSetters(mainSetter,node))
                            .reduce(InfluxFieldsCollector::reduceSetters).orElse(new ArrayList<>()));
                }
            }

        }
        if(list.size()==0)
            list.add(mainSetter.get());
        return list;
    }

    private static List<Function<Point.Builder,Point.Builder>> reduceSetters(List<Function<Point.Builder,Point.Builder>> l1,List<Function<Point.Builder,Point.Builder>> l2){
        List<Function<Point.Builder,Point.Builder>> start = l1.size()==0?l2:l1;
        if(l1.size()==0 && l2.size()==0) return new ArrayList<>();
        if(l1.size()==0 || l2.size()==0) return start;
        if(l1.size()==l2.size()){
            AtomicInteger index= new AtomicInteger();
            l1.replaceAll(b->b.andThen(l2.get(index.getAndIncrement())));
            return l1;
        }
        return l1.stream().flatMap(s->l2.stream().map(s::andThen)).collect(Collectors.toList());
    }

    private static Point.Builder setValueFromNode(Point.Builder builder, InfluxField field, JsonNode value){
        Object obj = FieldValueConvertor.convert(value);
        if(obj!=null){
            if(field.isTag())
                builder.tag(field.getName(),obj.toString());
            else if(field.isTime())
                builder.time(value.asLong(), TimeUnit.MILLISECONDS);
            else {
                Map<String,Object> map = new HashMap<>();
                map.put(field.getName(), obj);
                builder.fields(map);
            }
        }
        return builder;
    }



    /*
            P
        /   |   \
      C1   C2    C3
    / | \ / | \ / | \
   M  M M M M M M M  M
   D  D D D D D D D  D
   D  D D D D D D D  D
   D  D D D D D D D  D
   D  D D D D D D D  D





     */

}
