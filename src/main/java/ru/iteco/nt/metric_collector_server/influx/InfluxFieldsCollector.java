package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import ru.iteco.nt.metric_collector_server.collectors.web_client.Utils;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;

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
        List<Function<Point.Builder,Point.Builder>> list = new ArrayList<>();
        AtomicReference<Function<Point.Builder,Point.Builder>> root = new AtomicReference<>(b->b);
        addPointSetters(root,list,influxField,source);
        if(list.size()==0) {
            Point.Builder builder = root.get().apply(Point.measurement("test"));
            list.add(root.get());
        }
        return list;
    }

    private static void addPointSetters(AtomicReference<Function<Point.Builder,Point.Builder>> root,List<Function<Point.Builder,Point.Builder>> list,InfluxField influxField,JsonNode source){
        //if influxField has value (expecting primitive value or String) set in root setter.
        if(influxField.getValue()!=null && influxField.getValue().isValueNode()){
            root.set(root.get().andThen(p->setValue(p,influxField,influxField.getValue())));
        }
        //if influxField has path from source (expecting not empty node with value, array or object)
        if(influxField.getPath()!=null && !influxField.getPath().trim().isEmpty()){
            JsonNode node = Utils.getFromJsonNode(source,influxField.getPath());
            //expecting not empty node with value, array or object
            if(node!=null && (!node.isArray() || !node.isEmpty())){
                // No Children -  expecting single value or array with values
                if(influxField.getChildren()==null|| influxField.getChildren().isEmpty()){
                    // if array set or update setters of Point.Builder
                    if(node.isArray()){
                        //if list of Point.Builder setters is empty fill list using root setter
                        if(list.isEmpty()){
                            node.forEach(n->list.add(root.get().andThen(b->setValue(b,influxField,n))));
                        }
                        // if list is not empty expecting ArrayNode same size with list of Point.Builder setters, update setters with value from array
                        else if(node.size()==list.size()) {
                            AtomicInteger index = new AtomicInteger();
                            list.replaceAll(bs->bs.andThen(b->setValue(b,influxField,node.get(index.getAndIncrement()))));
                        }
                    } else if(node.isValueNode()){
                        //if list not set update root
                        if(list.isEmpty()){
                            root.set(root.get().andThen(b->setValue(b,influxField,node)));
                        }
                        //if list set update setter in list
                        else {
                            list.replaceAll(bs -> bs.andThen(b -> setValue(b, influxField, node)));
                        }
                    }
                }
                //if influxField has children set for each child new root and do recursive call
                else  {
                    influxField.getChildren().forEach(c->addPointSetters(new AtomicReference<>(root.get()),list,c,node));
                }
            }


        }

    }

    private static Point.Builder setValue(Point.Builder builder,InfluxField field,JsonNode value){
        if(field.isTag())
            builder.tag(field.getName(),value.asText());
        else if(field.isTime())
            builder.time(value.asLong(), TimeUnit.MILLISECONDS);
        else {
            Map<String,Object> map = new HashMap<>();
            map.put(field.getName(),FieldValueConvertor.convert(value));
            builder.fields(map);
        }
        return builder;
    }

    @RequiredArgsConstructor
    enum FieldValueConvertor {
        INT(JsonNode::isInt,JsonNode::asInt),
        LONG(JsonNode::isLong,JsonNode::asLong),
        DOUBLE(JsonNode::isDouble,JsonNode::asDouble),
        BOOLEAN(JsonNode::isBoolean,JsonNode::asBoolean),
        OBJECT(JsonNode::isObject,j->null),
        ARRAY(JsonNode::isArray,j->null),
        STRING(j->true,JsonNode::asText)
        ;

        private final Predicate<JsonNode> checker;
        private final Function<JsonNode,Object> convertor;

        static FieldValueConvertor getConvertor(JsonNode jsonNode){
            if(jsonNode==null)return null;
            return Arrays.stream(values()).filter(c->c.checker.test(jsonNode)).findFirst().orElse(STRING);
        }

        static Object convert(JsonNode jsonNode){
            return getConvertor(jsonNode).convertor.apply(jsonNode);
        }

        static Map<String,Object> getFields(List<InfluxField> fields){
            return fields.stream().filter(f->!f.isTag()).collect(Collectors.toMap(InfluxField::getName, f->convert(f.getValue())));
        }


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
