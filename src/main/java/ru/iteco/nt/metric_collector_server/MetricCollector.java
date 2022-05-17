package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MetricCollector<P,S,C extends MetricWriter<P,?,?,?>,T extends DataResponse<?> & ResponseWithMessage<T>> {

    private final static AtomicInteger ID_SOURCE = new AtomicInteger();

    @Getter
    private final int id;
    @Getter
    private final S config;
    @Getter
    private final C dbConnector;
    private Disposable disposable;

    protected MetricCollector(S config, C dbConnector) {
        this.config = config;
        this.dbConnector = dbConnector;
        id = ID_SOURCE.get();
    }

    public abstract T response();

    public Mono<T> responseMono(){
        return Mono.fromSupplier(this::response);
    }

    public void stop(){
        if(disposable!=null && !disposable.isDisposed())
            disposable.dispose();
        disposable = null;
    }

    public Mono<T> startCollecting(Flux<JsonNode> source){
        return isRunning() ?
                getWithMessage("already running") :
                Mono.fromRunnable(()->start(source)).then(getWithMessage("started"));
    }

    public void start(Flux<JsonNode> source){
        disposable = setUpCollectingDisposable(source);
    }

    public Disposable setUpCollectingDisposable(Flux<JsonNode> source) {
        return source.subscribe(data->{
            List<P> list = new ArrayList<>();
            Instant time = Instant.now();
            addPointFromData(data,list,time);
            dbConnector.addPoints(list);
        });
    }

    public abstract void addPointFromData(JsonNode data,List<P> list,Instant time);

    public List<P> getPointFromData(JsonNode data,Instant time){
        List<P> list = new ArrayList<>();
        addPointFromData(data,list,time);
        return list;
    }

    public List<P> getPointFromData(JsonNode data){
        return getPointFromData(data,Instant.now());
    }

    public Mono<T> stopCollecting(){
        return isRunning() ? Mono.fromRunnable(this::stop)
                .then(getWithMessage("stopped")) : getWithMessage("already stopped");
    }

    public Mono<T> getWithMessage(String message){
        return responseMono().map(t->t.setMessage(String.format("%s - %s",getClass().getSimpleName(),message)));
    }

    public boolean isRunning(){
        return disposable!=null && !disposable.isDisposed();
    }

    public abstract Mono<List<JsonNode>> validate();

    public abstract Mono<List<JsonNode>> validateData(JsonNode data);

}
