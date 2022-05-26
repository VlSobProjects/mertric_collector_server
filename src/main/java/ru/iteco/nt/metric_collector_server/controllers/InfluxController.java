package ru.iteco.nt.metric_collector_server.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.iteco.nt.metric_collector_server.influx.InfluxMetricService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxDbConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;


@Slf4j
@RestController
@RequestMapping("/influx")
public class InfluxController extends AbstractMetricWriterController<
        InfluxDBConnectorConfig,
        InfluxDbConnectorResponse,
        InfluxMetricCollectorConfig,
        InfluxMetricCollectorResponse,
        InfluxMetricCollectorGroupConfig,
        InfluxMetricCollectorGroupResponse,
        InfluxMetricService
        > {


    public InfluxController(InfluxMetricService metricService) {
        super(metricService);
    }

}
