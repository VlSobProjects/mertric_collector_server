package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import ru.iteco.nt.metric_collector_server.convarters.JsonValueConverterConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class InfluxField  implements HasJsonPath {
    private String path;
    private String name;
    private boolean tag;
    private boolean time;
    private JsonNode value;
    private Set<InfluxField> children;
    private JsonValueConverterConfig converterConfig;

    @JsonIgnore
    public boolean isNoChildren(){
        return children==null || children.isEmpty();
    }
    @JsonIgnore
    public boolean hasName(){
        return name!=null && !name.trim().isEmpty();
    }
    @JsonIgnore
    public boolean hasValue(){
        return value!=null && value.isValueNode() && !value.asText().trim().isEmpty();
    }

    @JsonIgnore
    public boolean isPointValue(){
        return !tag && !time && hasName();
    }

    @JsonIgnore
    public JsonNode shortVersion(){
        return ((ObjectNode)Utils.valueToTree(builder().value(value).name(name).path(path).tag(tag).time(time).build()))
                .put("children",isNoChildren()? 0 : children.size())
                .put("hasValue",hasValue())
                .put("fields",isNoChildren()? "no" : children.stream().map(InfluxField::getName).collect(Collectors.joining(", ")));
    }

    public boolean hasChild(InfluxField influxField){
        return !isNoChildren() && getChildren().contains(influxField);
    }

}
