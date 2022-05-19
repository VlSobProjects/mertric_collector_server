package ru.iteco.nt.metric_collector_server.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.iteco.nt.metric_collector_server.influx.InfluxMetricToFileService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxToFileConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxToFileConnectorConfig;

@RestController
@RequestMapping("/influxToFile")
public class InfluxToFileController extends AbstractMetricWriterController<
        InfluxToFileConnectorConfig,
        InfluxToFileConnectorResponse,
        InfluxMetricCollectorConfig,
        InfluxMetricCollectorResponse,
        InfluxMetricCollectorGroupConfig,
        InfluxMetricCollectorGroupResponse,
        InfluxMetricToFileService
        > {
    public InfluxToFileController(InfluxMetricToFileService metricService) {
        super(metricService);
    }
}
