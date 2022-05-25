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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
    private final AtomicBoolean validateData = new AtomicBoolean();
    private final AtomicBoolean validationDataPass = new AtomicBoolean();
    private final AtomicReference<List<JsonNode>> validationError = new AtomicReference<>();

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

    public boolean isDataValidated(){
        return validateData.get();
    }

    public boolean isValidateDataPass(){
        return validationDataPass.get();
    }

    protected void setValidationDataPass(boolean validationDataPass){
        this.validationDataPass.set(validationDataPass);
    }

    protected void setValidationData(boolean validationData){
        this.validateData.set(validationData);
    }

    public abstract T response();

    public Mono<T> responseMono(){
        return Mono.fromSupplier(this::response);
    }

    public synchronized void stop(){
        log.debug("stop isRunning() {}",isRunning());
        if(disposable!=null && !disposable.isDisposed())
            disposable.dispose();
        disposable = null;
    }

    public Mono<T> startCollecting(Flux<JsonNode> source){
        return isRunning() ?
                getWithMessage("already running") :
                Mono.fromRunnable(()->start(source)).then(getWithMessage("started"));
    }

    public synchronized void start(Flux<JsonNode> source){
        log.debug("start isRunning {}",isRunning());
        if(!isRunning()){
            disposable = setUpCollectingDisposable(source);
            log.debug("start new disposable is disposed: {}",isRunning());
       }

    }

    public Disposable setUpCollectingDisposable(Flux<JsonNode> source) {
        return source.subscribe(data->{
            List<P> list = new ArrayList<>();
            Instant time = Instant.now();
            addPointFromData(data,list,time);
            log.debug("Get data: {} list size:{}",data,list.size());
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
    
    public <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> validateAndSet(Mono<R> response, UnaryOperator<Mono<R>> addIfNotFail){
        return validateMono().flatMap(l->{
            if(isValidationFail(l)) return Utils.setMessageAndData(response,"Collector Filed Validation Fail",leaveOnlyAndSetErrorsData(l));
            else return addIfNotFail.apply(response);
        });
    }

    public <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> validateAndRemove(Mono<R> response, Mono<JsonNode> data, UnaryOperator<Mono<R>> removeIfFail){
        return validateAndDo(response,data,true,removeIfFail);
    }

    protected <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> validateAndDo(Mono<R> response, Mono<JsonNode> data, boolean doOnError , UnaryOperator<Mono<R>> doOn){
        return validateMono().flatMap(l->{
            if(isValidationFail(l)){
                Mono<R> r = Utils.setMessageAndData(response,"Collector Filed Validation Fail",leaveOnlyAndSetErrorsData(l));
                return doOnError ? doOn.apply(r) : r;
            } else return data.flatMap(j->{
                if((j==null || j.isEmpty() || j.has("error"))){
                    return doOnError ? failToCheckWarn(response,j) : doOn.apply(failToCheckWarn(response,j));
                } else {
                    Function<Boolean,Mono<R>> resp = isPass-> isPass ?
                            response :
                            Utils.setMessageAndData(response,"Collector Data Validation Fail",validationError.get());
                    return Mono.fromRunnable(()->validateDataAndSet(j)).then(
                            (doOnError && !isValidateDataPass()) || (!doOnError && isValidateDataPass()) ?
                                    doOn.apply(resp.apply(isValidateDataPass())) : resp.apply(isValidateDataPass()));
                }
            });
        });
    }

    public void validateDataAndSet(JsonNode data){
        List<JsonNode> r = validateData(data);
        validationDataPass.set(!isValidationFail(validateData(data)));
        if(!validationDataPass.get()){
            if(isRunning())
                stop();
            validationError.set(leaveOnlyAndSetErrorsData(r));
        } else validationError.set(null);
        validateData.set(true);
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

    public abstract Mono<List<JsonNode>> validateMono();

    public Mono<List<JsonNode>> validateDataMono(JsonNode data){
        return Mono.fromSupplier(()->validateData(data));
    }

    public abstract List<JsonNode> validateData(JsonNode data);

    public boolean isValidationFail(List<JsonNode> validationResult) {
        return validationResult.stream().anyMatch(j->j.has("error"));
    }

}
