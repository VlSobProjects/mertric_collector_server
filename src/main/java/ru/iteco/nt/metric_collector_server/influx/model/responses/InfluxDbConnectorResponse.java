package ru.iteco.nt.metric_collector_server.influx.model.responses;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;

@SuperBuilder
@Getter
public class InfluxDbConnectorResponse extends DataResponse<InfluxDBConnectorConfig> {
    private final boolean success;
    private final int queueSize;
    private final boolean writing;
    private final int lastDataSize;
    private final int maxDataSize;
    private int maxQueueSize;


    public static Mono<InfluxDbConnectorResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }
}
