package ru.iteco.nt.metric_collector_server.collectors.model.responses;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.collectors.web_client.Utils;

@Getter
@SuperBuilder
public abstract class ApiDataResponse<S> {

    private final int id;
    private final S settings;
    private final JsonNode data;
    private final long time;
    private final boolean fail;

    public static <S,T extends ApiDataResponse<S>> T factoryError(String source,String message,ApiDataResponseBuilder<S,T,?> builder,Object ...objects){
        return builder.fail(true).time(System.currentTimeMillis()).data(Utils.getError(source, message, objects)).build();
    }


}
