package ru.iteco.nt.metric_collector_server.influx.model.settings;


import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.MetricConfig;

@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class InfluxMetricCollectorGroupConfig extends MetricConfig {

}
