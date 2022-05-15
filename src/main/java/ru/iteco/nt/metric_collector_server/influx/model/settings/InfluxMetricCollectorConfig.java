package ru.iteco.nt.metric_collector_server.influx.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Setter
@Getter
public class InfluxMetricCollectorConfig extends InfluxMetricConfig {
    private Integer apiCollectorId;
    private Integer influxDbId;
    private List<InfluxField> fields;
    private String measurement;
    private String path;
    private boolean setTime;

    @JsonIgnore
    public JsonNode shortVersion(){
        return ((ObjectNode) Utils.valueToTree(InfluxMetricCollectorConfig
                .builder()
                .apiCollectorId(getApiCollectorId())
                .influxDbId(getInfluxDbId())
                .measurement(measurement)
                .path(path)
                .setTime(setTime)
        )).put("fields",fields.size());
    }

}
