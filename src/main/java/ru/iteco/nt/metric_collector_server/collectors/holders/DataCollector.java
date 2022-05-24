package ru.iteco.nt.metric_collector_server.collectors.holders;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.Comparator;

@Slf4j
@Getter
public abstract class DataCollector<R extends DataResponse<S>,S,B extends DataResponse.DataResponseBuilder<S,R,?>> extends ApiHolder<R,S,B> {

    private JsonNode data;
    private long time;
    private boolean fail;
    @Setter(AccessLevel.PROTECTED)
    private Runnable doOnError;
    @Setter(AccessLevel.PROTECTED)
    private Runnable doAfterError;

    protected DataCollector(S settings, int id) {
        super(settings, id);
    }


    public synchronized void setData(JsonNode jsonNode){
        boolean lasFail = fail;
        data = jsonNode;
        time = System.currentTimeMillis();
        fail = jsonNode.has("error");
        log.debug("set data: lasFail: {}, fail: {}, doOnError!=null: {}, doAfterFail!=null:{}",lasFail,fail,doOnError!=null,doAfterError!=null);
        if(fail && !lasFail && doOnError !=null)
            doOnError.run();
        if(!fail && lasFail && doAfterError !=null){
            doAfterError.run();
        }

    }

    public synchronized boolean isNotSameError(JsonNode jsonNode){
        if(!jsonNode.has("error") || !isFail()) return true;
        boolean is = jsonNode.get("error").get("errorMessage").equals(data.get("error").get("errorMessage"));
        log.debug("IS equals {}, cur error errorMessage:\n{}, new error errorMessage:\n{}",is,data.get("error").get("errorMessage"),jsonNode.get("error").get("errorMessage"));
        return !is;
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

    public void startOnError(){
        if(doOnError !=null) doOnError.run();
    }

    @Getter
    @RequiredArgsConstructor
    public static class ApiData {
        private final JsonNode data;
        private final long time;
        private final boolean fail;

        public JsonNode getNotNullData(){
            return data == null ? Utils.getError("DataCollector","daya is null") : data;
        }
    }
}
