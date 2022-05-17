package ru.iteco.nt.metric_collector_server.influx;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxToFileConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxToFileConfig;

@Service
public class InfluxMetricToFileService extends AbstractInfluxMetricService<InfluxToFileConfig, InfluxToFileConnectorResponse,InfluxToFileConnector> {


    protected InfluxMetricToFileService(ApiCollectorService apiCollectorService) {
        super(apiCollectorService, InfluxToFileConnector.class);
    }

    @Override
    protected Mono<InfluxToFileConnectorResponse> getErrorWriter(String message, Object... objects) {
        return InfluxToFileConnectorResponse.factoryError("InfluxMetricToFileService",message,objects);
    }

    @Override
    protected InfluxToFileConnector getWriter(InfluxToFileConfig config) {
        return new InfluxToFileConnector(config);
    }

}
