package ru.iteco.nt.metric_collector_server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum FieldValueConvertor {
    INT(JsonNode::isInt,JsonNode::asInt),
    LONG(JsonNode::isLong,JsonNode::asLong),
    DOUBLE(JsonNode::isDouble,JsonNode::asDouble),
    BOOLEAN(JsonNode::isBoolean,JsonNode::asBoolean),
    OBJECT(JsonNode::isObject,j->null),
    ARRAY(JsonNode::isArray,j->null),
    NULL(JsonNode::isNull,j->null),
    STRING(j->true,JsonNode::asText)
    ;

    private final Predicate<JsonNode> checker;
    private final Function<JsonNode,Object> convertor;

    public static FieldValueConvertor getConvertor(JsonNode jsonNode){
        if(jsonNode==null)return null;
        return Arrays.stream(values()).filter(c->c.checker.test(jsonNode)).findFirst().orElse(STRING);
    }

    public static Object convert(JsonNode jsonNode){
        return getConvertor(jsonNode).convertor.apply(jsonNode);
    }

    public static Map<String,Object> getFields(List<InfluxField> fields){
        return fields.stream().filter(f->!f.isTag()).collect(Collectors.toMap(InfluxField::getName, f->convert(f.getValue())));
    }
}
