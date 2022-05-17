package ru.iteco.nt.metric_collector_server.influx.model.responses;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;

import java.util.List;

@Getter
@SuperBuilder
public class InfluxMetricCollectorGroupResponse extends DataResponse<InfluxMetricCollectorGroupConfig>  implements ResponseWithMessage<InfluxMetricCollectorGroupResponse>{

    private final boolean collecting;
    private final WriterResponse<?> dbConnection;
    private final List<InfluxMetricCollectorResponse> collectors;
    private String message;

    public static Mono<InfluxMetricCollectorGroupResponse> factoryError(String source, String message, Object ...objects){
        return Mono.just(factoryError(source, message,builder(),objects));
    }

    public InfluxMetricCollectorGroupResponse setMessage(String message){
        this.message = message;
        return this;
    }
}
