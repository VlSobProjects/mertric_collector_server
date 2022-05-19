package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.MetricConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true,callSuper = false)
public class InfluxMetricCollectorConfig extends MetricConfig {
    private Integer apiCollectorId;
    private Integer influxDbId;
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
                .influxDbId(this.getWriterId())
                .measurement(measurement)
                .path(path)
                .setTime(setTime)
                .build()
        )).put("fields",fields.size());
    }

}
