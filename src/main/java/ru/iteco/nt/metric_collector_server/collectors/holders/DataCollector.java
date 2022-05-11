package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.collectors.web_client.Utils;

@Getter
public abstract class DataCollector<R extends DataResponse<S>,S,B extends DataResponse.DataResponseBuilder<S,R,?>> extends ApiHolder<R,S,B> {

    private JsonNode data;
    private long time;
    private boolean fail;
    @Setter(AccessLevel.PROTECTED)
    private Runnable startOnError;

    protected DataCollector(S settings, int id) {
        super(settings, id);
    }

    public synchronized void setData(JsonNode jsonNode){
        data = jsonNode;
        time = System.currentTimeMillis();
        fail = jsonNode.has("error");
        if(fail && startOnError!=null)
            startOnError.run();

    }

    @SuppressWarnings("unchecked")
    @Override
    protected synchronized B getSetBuilder() {
        return (B)super.getSetBuilder().data(data).fail(fail).time(time);
    }

    public synchronized ApiData getData(){
        return new ApiData(data,time,fail);
    }

    public static ApiData getErrorData(String source,String message,Object ... objects){
        return new ApiData(Utils.getError(source, message, objects),System.currentTimeMillis(),true);
    }

    @Getter
    @RequiredArgsConstructor
    public static class ApiData {
        private final JsonNode data;
        private final long time;
        private final boolean fail;
    }
}
