package ru.iteco.nt.metric_collector_server.collectors.model.responses;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCallConfig;

@Getter
@SuperBuilder
public class ApiCallResponse extends DataResponse<ApiCallConfig> {
    private final ApiCollectorResponse collectorResponse;
    private final boolean isChecking;

    public static Mono<ApiCallResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }

}
