package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.model.responses.ResponseWithMessage;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class MetricCollector<P,S extends MetricConfig,C extends MetricWriter<P,?,?,?>,T extends DataResponse<?> & ResponseWithMessage<T>> {

    private final static AtomicInteger ID_SOURCE = new AtomicInteger();

    @Getter
    private final int id;
    @EqualsAndHashCode.Include
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

    protected MetricCollector(S config){
        this.config = config;
        id = -1;
        dbConnector = null;
    }

    public abstract T response();

    public abstract boolean isValidationFail(List<JsonNode> validationResult);

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

    public Mono<T> responseWithValidate(){
        return validate().flatMap(l->responseMono().map(r-> {
            r.dataArray(l);
            return r;
        }));
    }

    public Mono<T> responseWithValidate(JsonNode data){
        return validateData(data).flatMap(l->responseMono().map(r-> {
            r.dataArray(l);
            return r;
        }));
    }

    public <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> validateAndSet(Mono<R> response, Mono<JsonNode> data, UnaryOperator<Mono<R>> addIfNotFail){
        return validate().flatMap(l->{
            if(isValidationFail(l)) return Utils.setMessageAndData(response,"Collector Validation Fail",config,l);
            else return data.flatMap(j->{
                if(j==null || j.isEmpty()) return addIfNotFail.apply(Utils.setMessageAndData(response,"Warn: data from ApiCall is null or empty"));
                else return validateData(j).flatMap(l2->{
                    if(isValidationFail(l2)) return Utils.setMessageAndData(response,"Collector Data Validation Fail",config,l2);
                    else return addIfNotFail.apply(response);
                });
            });
        });
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
