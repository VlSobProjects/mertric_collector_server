package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;


@Slf4j
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
        id = ID_SOURCE.incrementAndGet();
    }

    protected MetricCollector(S config,int id){
        this.config = config;
        this.id = id;
        dbConnector = null;
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

    public <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> validateAndSet(Mono<R> response, Mono<JsonNode> data, UnaryOperator<Mono<R>> addIfNotFail){
        return validateAndDo(response,data,false,addIfNotFail);
    }

    public <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> validateAndRemove(Mono<R> response, Mono<JsonNode> data, UnaryOperator<Mono<R>> removeIfFail){
        return validateAndDo(response,data,true,removeIfFail);
    }

    protected <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> validateAndDo(Mono<R> response, Mono<JsonNode> data, boolean doOnError , UnaryOperator<Mono<R>> doOn){
        return validate().flatMap(l->{
            if(isValidationFail(l)){
                Mono<R> r = Utils.setMessageAndData(response,"Collector Filed Validation Fail",leaveOnlyAndSetErrorsData(l));
                return doOnError ? doOn.apply(r) : r;
            } else return data.flatMap(j->{
                if((j==null || j.isEmpty() || j.has("error"))){
                    return doOnError ? failToCheckWarn(response,j) : doOn.apply(failToCheckWarn(response,j));
                } else return validateData(j).flatMap(l2->{
                    Mono<R> r = isValidationFail(l2) ?
                            Utils.setMessageAndData(response,"Collector Data Validation Fail",leaveOnlyAndSetErrorsData(l2)) :
                            response;
                    return (doOnError && isValidationFail(l2)) || (!doOnError && !isValidationFail(l2)) ? doOn.apply(r) : r;
                });
            });
        });
    }

    protected <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> failToCheckWarn(Mono<R> response,JsonNode error){
        return Utils.setMessageAndData(response,"Warn: Fail to check Data",Utils.getWarn(getClass().getSimpleName(),"Warn Data: data from ApiCall is null or empty or error",error,response()));
    }

    protected List<JsonNode> leaveOnlyAndSetErrorsData(List<JsonNode> validationResult){
        return Utils.collectDataToList(j->true, validationResult.stream().filter(j->j.has("error")).map(j->j.get("error").get("data")).collect(Collectors.toList()));
    }

    public Mono<T> getWithMessage(String message){
        return responseMono().map(t->t.setMessage(String.format("%s - %s",getClass().getSimpleName(),message)));
    }

    public boolean isRunning(){
        return disposable!=null && !disposable.isDisposed();
    }

    public abstract Mono<List<JsonNode>> validate();

    public abstract Mono<List<JsonNode>> validateData(JsonNode data);

    public boolean isValidationFail(List<JsonNode> validationResult) {
        return validationResult.stream().anyMatch(j->j.has("error"));
    }

}
