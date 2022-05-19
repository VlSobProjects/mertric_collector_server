package ru.iteco.nt.metric_collector_server.influx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxToFileConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxToFileConnectorConfig;

@Slf4j
@Service
public class InfluxMetricToFileService extends AbstractInfluxMetricService<InfluxToFileConnectorConfig, InfluxToFileConnectorResponse,InfluxToFileConnector> {


    protected InfluxMetricToFileService(ApiCollectorService apiCollectorService) {
        super(apiCollectorService, InfluxToFileConnector.class);
    }

    @Override
    protected Mono<InfluxToFileConnectorResponse> getErrorWriter(String message, Object... objects) {
        return InfluxToFileConnectorResponse.factoryError("InfluxMetricToFileService",message,objects);
    }

    @Override
    protected InfluxToFileConnector getWriter(InfluxToFileConnectorConfig config) {
        log.info("InfluxToFileConnectorConfig: {}",config);
        return new InfluxToFileConnector(config);
    }

}
