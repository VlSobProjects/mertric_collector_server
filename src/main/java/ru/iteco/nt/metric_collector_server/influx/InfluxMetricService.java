package ru.iteco.nt.metric_collector_server.influx;


import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxDbConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;



@Service
public class InfluxMetricService extends AbstractInfluxMetricService<InfluxDBConnectorConfig,InfluxDbConnectorResponse,InfluxDbConnector> {

    protected InfluxMetricService(ApiCollectorService apiCollectorService) {
        super(apiCollectorService, InfluxDbConnector.class);
    }
    
    @Override
    protected Mono<InfluxDbConnectorResponse> getErrorWriter(String message, Object... objects) {
        return InfluxDbConnectorResponse.factoryError("InfluxMetricService",message,objects);
    }

    @Override
    protected InfluxDbConnector getWriter(InfluxDBConnectorConfig config) {
        return new InfluxDbConnector(config);
    }


}
