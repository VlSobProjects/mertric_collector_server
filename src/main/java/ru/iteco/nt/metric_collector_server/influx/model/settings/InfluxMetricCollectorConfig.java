package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.MetricConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true,callSuper = false)
public class InfluxMetricCollectorConfig extends MetricConfig {

    @EqualsAndHashCode.Include
    private Set<InfluxField> fields;
    @EqualsAndHashCode.Include
    private String measurement;
    @EqualsAndHashCode.Include
    private String path;
    private boolean setTime;

    @JsonIgnore
    public JsonNode shortVersion(){
        return ((ObjectNode) Utils.valueToTree(InfluxMetricCollectorConfig
                .builder()
                .apiCollectorId(getApiCollectorId())
                .writerId(this.getWriterId())
                .measurement(measurement)
                .path(path)
                .setTime(setTime)
                .build()
        )).put("fields", isNoFields()? fields.size(): 0).put("fields",isNoFields()? "no" : fields.stream().map(InfluxField::getPath).filter(Objects::nonNull).collect(Collectors.joining(", ")));
    }
    private boolean isNoFields(){
        return fields==null || fields.size()==0;
    }

}
