package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;
import ru.iteco.nt.metric_collector_server.utils.FieldValueConvertor;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class InfluxFieldsCollector {

    private final InfluxField influxField;

    private static final BiPredicate<JsonNode,InfluxField> VALIDATE_NODE = (j,f)->f.hasPath() && Utils.validatePath(j,f.getPath());

    private static final Predicate<InfluxField> ERROR_VALIDATE_FLAGS = f->f.isTime() && f.isTag();
    private static final String ERROR_FLAG_MESSAGE = "Field can't be tag and time at same time";


    public List<Function<Point.Builder,Point.Builder>> getPointSetters(JsonNode source){
        return getFieldSetters(new AtomicReference<>(b->b),source);
    }

    public List<Function<Point.Builder,Point.Builder>> getFieldSetters(AtomicReference<Function<Point.Builder,Point.Builder>> mainSetter,JsonNode source){
        List<Function<Point.Builder,Point.Builder>> list = new ArrayList<>();
        //set static field to Point.Builder setter if exist - expecting value Node
        if(influxField.hasValue()){
            mainSetter.set(mainSetter.get().andThen(b->setValueFromNode(b,influxField,influxField.getValue())));
        }
        //get Node by path if path exist
        if(influxField.hasPath()){
            JsonNode node = Utils.getFromJsonNode(source,influxField.getPath());
            //if no children set up extracted by path field to Point.Builder setter expecting value node or array of value nodes
            if(influxField.isNoChildren()){
                //set up value to Point.Builder setter
                if(node.isValueNode() && !node.isNull()){
                    mainSetter.set(mainSetter.get().andThen(b->setValueFromNode(b,influxField,node)));
                }
                //if node is array of value add create from Point.Builder setter setters for each value and add to list
                else if(node.isArray()){
                    node.forEach(n-> {
                        if(n.isValueNode() && !n.isNull())
                            list.add(mainSetter.get().andThen(b -> setValueFromNode(b, influxField, n)));
                    });
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
                                    .reduce((l1,l2)->Utils.reduceSetters(l1,l2,n))
                                    .orElse(new ArrayList<>())
                            )
                    );
                    //if node is not array - expecting object, collecting all fields form object and set it together
                } else {
                    list.addAll(influxField.getChildren().stream().map(InfluxFieldsCollector::new)
                            .map(c->c.getFieldSetters(mainSetter,node))
                            .reduce((l1,l2)->Utils.reduceSetters(l1,l2,node))
                            .orElse(new ArrayList<>()));
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

    public Mono<JsonNode> validate(){
        return Mono.fromSupplier(()->{
            List<JsonNode> errors = new ArrayList<>();
            influxFieldValidation(influxField,errors::add);
            if(errors.isEmpty()) return Utils.getObjectNode("message","InfluxField Check - No error found").set("filed",influxField.shortVersion());
            else return Utils.getError("InfluxFieldsCollector.validate","Influx Filed config Validation Fail",errors.toArray());
        });
    }

    public JsonNode validateData(JsonNode data){
        List<JsonNode> errors = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        dataValidateAndCount(data,influxField,count,errors::add);
        if(errors.isEmpty()){
            int settersCount = getPointSetters(data).size();
            if(settersCount!=count.get()){
                return Utils.getObjectNode("message",String.format("Data InfluxField Check - No error found, One warning: Number of Setter: %s not equal dataValidateAndCount result: %s.",settersCount,count.get()))
                        .set("filed",influxField.shortVersion());
            } else return Utils.getObjectNode("message","Data InfluxField Check - No error found, No warning.")
                    .set("filed",influxField.shortVersion());
        } else return Utils.getError("InfluxFieldsCollector.validateData","Influx Data config Validation Fail",errors.toArray());
    }


    private static int countMax(JsonNode node,Collection<InfluxField> fields,Consumer<JsonNode> error){
        return fields.stream().mapToInt(f->{
            if(!validatePath(node,f,error)) return 0;
            JsonNode n = Utils.getFromJsonNode(node,f.getPath());
            if(n.isArray()) {
                return IntStream.range(0,n.size()).map(i->n.get(i).isNull()?0:1).sum();
            } else return n.isNull()?0:1;
        }).max().orElse(0);
    }

    private static boolean validatePath(JsonNode data, InfluxField field,Consumer<JsonNode> error){
        boolean is = !field.hasPath() || VALIDATE_NODE.test(data,field);
        if(!is) error.accept(Utils.getError("InfluxFieldsCollector.influxFiledDataError","Utils.validatePath fail JsonNode by field path null or empty or missing or contains only nulls",field.shortVersion(),data));
        return is;
    }

    private static void dataValidateAndCount(JsonNode data, InfluxField field, AtomicInteger count, Consumer<JsonNode> error){
        validatePath(data, field, error);
        if(field.isNoChildren())count.compareAndSet(0,1);
        else {
            JsonNode node = Utils.getFromJsonNode(data,field.getPath());
            if(field.getChildren().stream().allMatch(InfluxField::isNoChildren)){
                if(node.isArray()){
                    node.forEach(n-> count.addAndGet(countMax(n, field.getChildren(),error)));
                } else {
                    count.addAndGet(countMax(node,field.getChildren(),error));
                }
            } else {
                if(node.isArray()){
                    field.getChildren().stream().filter(c->!c.isNoChildren()).forEach(f->node.forEach(n-> dataValidateAndCount(n,f,count,error)));
                } else field.getChildren().stream().filter(c->!c.isNoChildren()).forEach(f-> dataValidateAndCount(node,f,count,error));
            }
        }
    }

    @RequiredArgsConstructor
    private enum InfluxFiledValidator{
        TAG(InfluxField::isTag,f->(!f.hasValue() && !f.hasPath()) || !f.hasName(),"Field is tag but value or path not exist or name is not present"),
        TIME(InfluxField::isTime,f->!f.hasPath(),"Field is time but path not exist"),
        VALUE(InfluxField::isPointValue,TAG.errorCondition,"Field is value but value or path not exist or name is not present"),
        NO_PARENT(InfluxField::isNoChildren,TAG.fieldCondition.or(VALUE.fieldCondition).or(TIME.fieldCondition).negate(),"Field has no children but it is not tag or value or time"),
        PARENT(NO_PARENT.fieldCondition.negate(),f->!f.hasPath(),"Field has children but no path")
        ;
        private final Predicate<InfluxField> fieldCondition;
        private final Predicate<InfluxField> errorCondition;
        private final String errorMassage;

        private static void influxFieldError(InfluxField field,String message,Consumer<JsonNode> error){
            error.accept(Utils.getError("InfluxFiledValidator",message,field.shortVersion()));
        }
        private static void validate(InfluxField field,Consumer<JsonNode> error){
            if(ERROR_VALIDATE_FLAGS.test(field))
                influxFieldError(field,ERROR_FLAG_MESSAGE,error);
            Arrays.stream(values()).forEach(v->v.validateField(field, error));
        }

        private void validateField(InfluxField field,Consumer<JsonNode> error){
            if(fieldCondition.test(field) && errorCondition.test(field))
                influxFieldError(field,errorMassage,error);
        }

    }

    private static void influxFieldValidation(InfluxField influxField,Consumer<JsonNode> error){
        InfluxFiledValidator.validate(influxField, error);
        if(!influxField.isNoChildren())
            influxField.getChildren().forEach(f->influxFieldValidation(f,error));
    }

}
