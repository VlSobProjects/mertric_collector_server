package ru.iteco.nt.metric_collector_server.influx.model.settings;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.MetricConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class InfluxMetricCollectorGroupConfig extends MetricConfig {

    @Override
    public JsonNode shortVersion() {
        return Utils.valueToTree(this);
    }
}
