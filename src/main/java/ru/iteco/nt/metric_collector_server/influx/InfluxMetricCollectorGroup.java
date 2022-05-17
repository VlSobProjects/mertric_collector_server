package ru.iteco.nt.metric_collector_server.influx;

import org.influxdb.dto.Point;

import ru.iteco.nt.metric_collector_server.MetricCollectorGroup;
import ru.iteco.nt.metric_collector_server.MetricWriter;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;

import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;

import java.util.stream.Collectors;

public class InfluxMetricCollectorGroup extends MetricCollectorGroup<
        Point
        ,InfluxMetricCollectorGroupConfig
        ,InfluxMetricCollectorGroupResponse
        ,MetricWriter<Point,?,?,?>
        ,InfluxMetricCollector
        > {

    public InfluxMetricCollectorGroup(InfluxMetricCollectorGroupConfig config,MetricWriter<Point,?,?,?> dbConnector) {
        super(config,dbConnector);
    }

    @Override
    public InfluxMetricCollectorGroupResponse response() {
        return InfluxMetricCollectorGroupResponse.builder()
                .time(System.currentTimeMillis())
                .collecting(isRunning())
                .dbConnection(getDbConnector().response())
                .collectors(getCollectors().stream().map(InfluxMetricCollector::response).collect(Collectors.toList()))
                .id(getId())
                .settings(getConfig())
                .build()
                ;
    }


}
