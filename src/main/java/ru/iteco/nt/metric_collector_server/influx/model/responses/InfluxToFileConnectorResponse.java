package ru.iteco.nt.metric_collector_server.influx.model.responses;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxToFileConfig;

@SuperBuilder
@Getter
public class InfluxToFileConnectorResponse extends WriterResponse<InfluxToFileConfig> {

    public static Mono<InfluxToFileConnectorResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }
}
