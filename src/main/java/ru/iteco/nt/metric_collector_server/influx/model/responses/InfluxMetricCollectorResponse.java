package ru.iteco.nt.metric_collector_server.influx.model.responses;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;

@SuperBuilder
@Setter
public class InfluxMetricCollectorResponse extends DataResponse<InfluxMetricCollectorConfig> {

    private final boolean collecting;

    public static Mono<InfluxMetricCollectorResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }

}
