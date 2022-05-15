package ru.iteco.nt.metric_collector_server.influx.model.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Setter
@Getter
public abstract class InfluxMetricConfig {
    private Integer apiCollectorId;
    private Integer influxDbId;
}
