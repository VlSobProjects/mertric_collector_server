package ru.iteco.nt.metric_collector_server.influx;

import org.influxdb.dto.Point;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.MetricService;
import ru.iteco.nt.metric_collector_server.MetricWriter;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.WriterResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.WriterConfig;


public abstract class AbstractInfluxMetricService<SW extends WriterConfig
        ,RW extends WriterResponse<SW>
        ,W extends MetricWriter<Point,SW,RW,?>>
        extends MetricService<Point
        ,SW
        ,RW
        ,W
        ,InfluxMetricCollectorConfig
        ,InfluxMetricCollectorResponse
        ,InfluxMetricCollector
        ,InfluxMetricCollectorGroupConfig
        ,InfluxMetricCollectorGroupResponse
        ,InfluxMetricCollectorGroup
        > {
    protected AbstractInfluxMetricService(ApiCollectorService apiCollectorService, Class<W> writerClass) {
        super(apiCollectorService, writerClass, InfluxMetricCollectorGroup.class, InfluxMetricCollector.class);
    }

    @Override
    protected Mono<InfluxMetricCollectorResponse> getErrorCollector(String message, Object... objects) {
        return InfluxMetricCollectorResponse.factoryError("InfluxMetricService",message,objects);
    }
    @Override
    protected Mono<InfluxMetricCollectorGroupResponse> getErrorGroupCollector(String message, Object... objects) {
        return InfluxMetricCollectorGroupResponse.factoryError("InfluxMetricService",message,objects);
    }

    @Override
    protected InfluxMetricCollector getCollector(InfluxMetricCollectorConfig config, MetricWriter<Point,?,?,?> writer) {
        return new InfluxMetricCollector(config,writer);
    }

    @Override
    protected InfluxMetricCollector getCollector(InfluxMetricCollectorConfig config,int id) {
        return new InfluxMetricCollector(config,id);
    }

    @Override
    protected InfluxMetricCollectorGroup getGroupCollector(InfluxMetricCollectorGroupConfig config, MetricWriter<Point,?,?,?> writer) {
        return new InfluxMetricCollectorGroup(config,writer);
    }
}
