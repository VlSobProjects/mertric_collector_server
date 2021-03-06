package ru.iteco.nt.metric_collector_server.influx.model.responses;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;

@SuperBuilder
@Getter
public class InfluxDbConnectorResponse extends WriterResponse<InfluxDBConnectorConfig> {

    public static Mono<InfluxDbConnectorResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }
}
