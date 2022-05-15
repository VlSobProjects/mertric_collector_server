package ru.iteco.nt.metric_collector_server.influx;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;

public interface InfluxCollector<T extends DataResponse<?> & ResponseWithMessage<T>> {

    T response();

    Disposable getDisposable();

    void setDisposable(Disposable disposable);

    Disposable setUpCollectingDisposable(Flux<JsonNode> source);

    default void stop(){
        Disposable disposable = getDisposable();
        if(disposable!=null && !disposable.isDisposed())
            getDisposable().dispose();
        setDisposable(null);
    }

    default T startCollecting(Flux<JsonNode> source){
        if(isRunning()) return getWithMessage("already running");
        setDisposable(setUpCollectingDisposable(source));
        return getWithMessage("started");
    }

    default T stopCollecting(){
        if(isRunning()){
            stop();
            return getWithMessage("stopped");
        } else return getWithMessage("already stopped");
    }

    default T getWithMessage(String message){
        return response().setMessage(String.format("%s - %s",getClass().getSimpleName(),message));
    }

    default boolean isRunning(){
        Disposable disposable = getDisposable();
        return disposable!=null && !disposable.isDisposed();
    }

}
