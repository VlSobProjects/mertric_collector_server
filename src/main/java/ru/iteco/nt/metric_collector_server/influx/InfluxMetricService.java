package ru.iteco.nt.metric_collector_server.influx;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxDbConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InfluxMetricService {


    private final Map<Integer,InfluxDbConnector> influxDbConnectorMap = new ConcurrentHashMap<>();




    public Mono<InfluxDbConnectorResponse> addConnector(InfluxDBConnectorConfig config){
        return isConnectorExist(config) ? InfluxDbConnectorResponse
                .factoryError("InfluxMetricService.addConnector","InfluxDBConnector with same config exists",getResponseByConfig(config)) :
                Mono.fromSupplier(()->{
                    InfluxDbConnector connector = new InfluxDbConnector(config);
                    influxDbConnectorMap.put(connector.getId(),connector);
                    return connector.response();
                });
    }


    private boolean isConnectorExist(InfluxDBConnectorConfig config){
        return influxDbConnectorMap.values().stream().anyMatch(c->c.isSameConfig(config));
    }

    private InfluxDbConnectorResponse getResponseByConfig(InfluxDBConnectorConfig config){
        return influxDbConnectorMap.values().stream().filter(c->c.isSameConfig(config)).findFirst().map(InfluxDbConnector::response).orElse(null);
    }
}
