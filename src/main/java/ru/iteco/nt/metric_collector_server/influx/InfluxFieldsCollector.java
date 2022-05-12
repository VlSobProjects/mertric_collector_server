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
        //set static field to Point.Builder setter if exist - expecting value Node
        if(influxField.getValue()!=null && influxField.getValue().isValueNode()){
            mainSetter.set(mainSetter.get().andThen(b->setValueFromNode(b,influxField,influxField.getValue())));
        }
        //get Node by path if path exist
        if(influxField.getPath()!=null && !influxField.getPath().trim().isEmpty()){
            JsonNode node = Utils.getFromJsonNode(source,influxField.getPath());
            //if no children set up extracted by path field to Point.Builder setter expecting value node or array of value nodes
            if(influxField.getChildren()==null || influxField.getChildren().isEmpty()){
                //set up value to Point.Builder setter
                if(node.isValueNode()){
                    mainSetter.set(mainSetter.get().andThen(b->setValueFromNode(b,influxField,node)));
                }
                //if node is array of value add create from Point.Builder setter setters for each value and add to list
                else if(node.isArray()){
                    node.forEach(n->list.add(mainSetter.get().andThen(b->setValueFromNode(b,influxField,n))));
                }
                //if children exist
            } else {
                //if node is array node collect Point.Builder setters of children from each node in array
                if(node.isArray()){
                    node.forEach(n->
                            list.addAll(influxField.getChildren()
                                    .stream()
                                    .map(InfluxFieldsCollector::new)
                                    //create Point.Builder setters from new instance of Point.Builder setter so every node in array will set up new Point.Builder setter
                                    .map(c -> c.getFieldSetters(new AtomicReference<>(mainSetter.get()), n))
                                    //for each node combine all Point.Builder setters from children together
                                    .reduce(Utils::reduceSetters)
                                    .orElse(new ArrayList<>())
                            )
                    );
                    //if node is not array - expecting object, collecting all fields form object and set it together
                } else {
                    list.addAll(influxField.getChildren().stream().map(InfluxFieldsCollector::new)
                            .map(c->c.getFieldSetters(mainSetter,node))
                            .reduce(Utils::reduceSetters).orElse(new ArrayList<>()));
                }
            }

        }
        //if field has is single data(not array) list will be empty and mainSetter will set single Point.Builder. Add it to the list to return as result
        if(list.size()==0)
            list.add(mainSetter.get());
        return list;
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

}
