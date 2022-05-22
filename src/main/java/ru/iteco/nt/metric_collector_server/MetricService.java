package ru.iteco.nt.metric_collector_server;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.holders.ApiCollectorHolder;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.*;
import ru.iteco.nt.metric_collector_server.influx.model.settings.WriterConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class MetricService<
        P
        ,SW extends WriterConfig
        ,RW extends WriterResponse<SW>
        ,W extends MetricWriter<P,SW,RW,?>
        ,SC extends MetricConfig
        ,RC extends DataResponse<?> & ResponseWithMessage<RC>
        ,C extends MetricCollector<P,SC,? extends MetricWriter<P,?,?,?>,RC>
        ,SG extends MetricConfig
        ,RG extends DataResponse<SG> & ResponseWithMessage<RG>
        ,G extends MetricCollectorGroup<P,SG,RG,? extends MetricWriter<P,?,?,?>,C>
        > {

    private final ApiCollectorService apiCollectorService;
    private static final Map<Integer, MetricWriter<?,?,?,?>> METRIC_WRITER_MAP = new ConcurrentHashMap<>();
    private static final Map<Integer, MetricCollector<?,?,?,?>> METRIC_COLLECTOR_MAP = new ConcurrentHashMap<>();
    private final Class<W> writerClass;
    private final Class<G> collectorGroupClass;
    private final Class<C> collectorClass;

    protected MetricService(ApiCollectorService apiCollectorService, Class<W> writerClass, Class<G> collectorGroupClass, Class<C> collectorClass) {
        this.apiCollectorService = apiCollectorService;
        this.writerClass = writerClass;
        this.collectorGroupClass = collectorGroupClass;
        this.collectorClass = collectorClass;
    }
    protected abstract Mono<RW> getErrorWriter(String message, Object...objects);
    protected abstract Mono<RC> getErrorCollector(String message, Object...objects);
    protected abstract Mono<RG> getErrorGroupCollector(String message, Object...objects);
    protected abstract W getWriter(SW config);
    protected abstract C getCollector(SC config,MetricWriter<P,?,?,?> writer);
    protected abstract C getCollector(SC config,int id);
    protected abstract G getGroupCollector(SG config,MetricWriter<P,?,?,?> writer);

    public static Mono<List<WriterResponse<?>>> getAllWriters(){
        return Mono.fromSupplier(()->METRIC_WRITER_MAP.values().stream().map(MetricWriter::response).collect(Collectors.toList()));
    }
    public static Mono<List<DataResponse<?>>> getAllCollectors(){
        return Mono.fromSupplier(()->METRIC_COLLECTOR_MAP.values().stream().map(MetricCollector::response).collect(Collectors.toList()));
    }
    public static Mono<Void> stopAll(){
        return Mono.fromRunnable(()->{
            METRIC_COLLECTOR_MAP.values().forEach(MetricCollector::stop);
            METRIC_WRITER_MAP.values().forEach(MetricWriter::stop);
        });
    }

    public Mono<RW> addConnector(SW config){
        return isConnectorExist(config) ? getErrorWriter("Config exist in METRIC_WRITER_MAP",config,getResponseByConfig(config)) :
                Mono.fromSupplier(()->{
                    W writer = getWriter(config);
                    METRIC_WRITER_MAP.put(writer.getId(),writer);
                    return writer.response();
                });
    }

    public Mono<RW> startConnectorById(int connectorId){
        return getWriterFromMap(connectorId)
                .map(MetricWriter::startWrite)
                .orElseGet(()-> getErrorWriter("Metric Writer not found by id: "+connectorId));
    }

    public Mono<RW> stopConnectorById(int connectorId){
        return getWriterFromMap(connectorId)
                .map(MetricWriter::stopWrite)
                .orElseGet(()-> getErrorWriter("Metric Writer not found by id: "+connectorId));
    }

    public Mono<RW> getDbConnectorById(int connectorId){
        return getWriterFromMap(connectorId)
                .map(MetricWriter::responseMono)
                .orElseGet(()-> getErrorWriter("Metric Writer not found by id: "+connectorId));
    }

    public Mono<List<RW>> getAllServiceWriters(){
        return Mono.fromSupplier(()->getWriters().map(MetricWriter::response).collect(Collectors.toList()));
    }

    public Mono<ApiCollectorResponse> addSingleCollector(SC config){
        return addCollector(config, this::getCollector);
    }

    public Mono<ApiCollectorResponse> addGroupCollector(SG config){
        return addCollector(config,this::getGroupCollector);
    }

    public Mono<RG> addCollectorToGroup(int groupId, SC config){
        return  getCollectorGroupFromMap(groupId).map(c->{
            ApiCollectorHolder holder = apiCollectorService.getApiCollectorById(c.getConfig().getApiCollectorId()).orElse(null);
            if(holder==null) return Utils.setMessageAndData(c.responseMono(),"Unexpected ApiCollectorHolder not found in apiCollectorService",c.getConfig());
            else return c.validateAndAdd(holder.getApiCallHolder().lastApiCall(),config,conf->getCollector(conf,c.getCollectorId()));
        }).orElse(getErrorGroupCollector("Metric Collector Group not found by id: "+groupId));
    }

    public Mono<RG> stopGroupById(int groupId){
        return getCollectorGroupFromMap(groupId)
                .map(MetricCollector::stopCollecting)
                .orElseGet(()->getErrorGroupCollector("MetricCollectorGroup not found by id: "+groupId));
    }

    public Mono<RG> startGroupById(int groupId){
        return getCollectorGroupFromMap(groupId)
                .flatMap(c->apiCollectorService
                        .getApiCollectorById(c.getConfig().getApiCollectorId())
                        .map(h->c.startCollecting(h.getCollector()))
                ).orElseGet(()->getErrorGroupCollector("MetricCollectorGroup not found by id: "+groupId));
    }

    public Mono<RC> stopSingleCollectorById(int collectorId){
        return getCollectorFromMap(collectorId)
                .map(MetricCollector::stopCollecting)
                .orElseGet(()->getErrorCollector("MetricCollector not found by id: "+collectorId));
    }

    public Mono<RC> startCollectorById(int collectorId){
        return getCollectorFromMap(collectorId)
                .flatMap(c->apiCollectorService
                        .getApiCollectorById(c.getConfig().getApiCollectorId())
                        .map(h->c.startCollecting(h.getCollector()))
                ).orElseGet(()->getErrorCollector("MetricCollectorGroup not found by id: "+collectorId));
    }

    public Mono<RG> validateGroup(int groupId){
        return getCollectorGroupFromMap(groupId)
                .map(c->getCollectorHolderOrError(
                        c.responseMono()
                        ,c.getConfig().getApiCollectorId()
                        ,h->c.validateAndRemove(h.getApiCallHolder().lastApiCall()))
                ).orElse(getErrorGroupCollector("Metric Collector Group not found by id: "+groupId));
    }

    public Mono<RC> validateCollector(int collectorId){
        UnaryOperator<Mono<RC>> removeIfError = rc->{
            METRIC_COLLECTOR_MAP.remove(collectorId);
            return Utils.setMessageAndData(rc,"Validation Fail - Collector removed");
        };
        return getCollectorFromMap(collectorId)
                .map(c->getCollectorHolderOrError(c.responseMono(),c.getConfig().getApiCollectorId(),h->
                    c.validateAndRemove(c.responseMono(),h.getApiCallHolder().lastApiCall(),removeIfError)
                )).orElse(getErrorCollector("Metric Collector not found by id: "+collectorId));
    }

    public static Mono<Void> stopAndClearAll(){
        return stopAll().then(Mono.fromRunnable(()->{
            METRIC_COLLECTOR_MAP.clear();
            METRIC_WRITER_MAP.clear();
        }));
    }

    private boolean isConnectorExist(SW config){
        return METRIC_WRITER_MAP.values().stream().filter(writerClass::isInstance).anyMatch(c->c.isSameConfig(config));
    }

    private Optional<W> getWriterFromMap(int connectorId){
        return getByIdFormMap(connectorId,writerClass,METRIC_WRITER_MAP);
    }

    private Optional<G> getCollectorGroupFromMap(int collectorGroupId){
        return getByIdFormMap(collectorGroupId,collectorGroupClass,METRIC_COLLECTOR_MAP);
    }

    private Optional<C> getCollectorFromMap(int collectorId){
        return getByIdFormMap(collectorId,collectorClass,METRIC_COLLECTOR_MAP);
    }

    @SuppressWarnings("unchecked")
    private static <V,T extends V> Optional<T> getByIdFormMap(int id,Class<T> tClass,Map<Integer,V> map) {
        return Optional.ofNullable(map.get(id)).map(w-> {
            if(!tClass.isInstance(w)){
               log.error("getByIdFormMap object: {} class: {} isInstance fail!",w,tClass);
               return null;
            } else return (T) w;
        });
    }

    private RW getResponseByConfig(SW config){
        return getWriters()
                .filter(w->w.isSameConfig(config))
                .findFirst().map(MetricWriter::response)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Stream<W> getWriters(){
        return METRIC_WRITER_MAP.values()
                .stream()
                .filter(writerClass::isInstance)
                .map(w-> (W) w);
    }

    private static  <R extends DataResponse<?> & ResponseWithMessage<R>,T extends MetricConfig,W extends MetricWriter<?,?,?,?>> Mono<R> addToCollectorMap(Mono<R> response, T config, W connector, BiFunction<T,W, MetricCollector<?,?,?,?>> creator, AtomicReference<MetricCollector<?,?,?,?>> collectorRef){
        if(METRIC_COLLECTOR_MAP.values().stream().anyMatch(c->c.getConfig().equals(config)))
            return Utils.setData(response
                    ,Utils.getError("MetricService","Error: Duplicated config collector",config)
            );
        else {
            MetricCollector<?,?,?,?> collector = creator.apply(config,connector);
            METRIC_COLLECTOR_MAP.put(collector.getId(),collector);
            collectorRef.set(collector);
            return Utils.setMessageAndData(response,"Single Collector added.");
        }
    }

    private <R extends DataResponse<?> & ResponseWithMessage<R>> Mono<R> getCollectorHolderOrError(Mono<R> response, int apiCollectorId, Function<ApiCollectorHolder,Mono<R>> function){
        ApiCollectorHolder holder = apiCollectorService.getApiCollectorById(apiCollectorId).orElse(null);
        if(holder==null) return Utils.setMessageAndData(response,"Unexpected ApiCollectorHolder not found in apiCollectorService");
        else return function.apply(holder);
    }

    private <T extends MetricConfig> Mono<ApiCollectorResponse> addCollector(T config, BiFunction<T,W, MetricCollector<?,?,?,?>> creator){
        if(config.getApiCollectorId()==null)
            return ApiCollectorResponse.factoryError("MetricService.addCollector","not set apiCollectorId",config);
        if(config.getWriterId()==null)
            return ApiCollectorResponse.factoryError("MetricService.addCollector","not set MetricWriter for MetricCollector",config);
        W connector = getWriterFromMap(config.getWriterId()).orElse(null);
        if(connector==null)
            return ApiCollectorResponse.factoryError("MetricService.addCollector",String.format("not found MetricWriter by Id: %s, set up MetricWriter first",config.getWriterId()),config);
        ApiCollectorHolder collectorHolder = apiCollectorService.getApiCollectorById(config.getApiCollectorId()).orElse(null);
        if(collectorHolder==null)
            return ApiCollectorResponse.factoryError("MetricService.addCollector","ApiCollectorHolder not found by id: "+config.getApiCollectorId(),config);
        AtomicReference<MetricCollector<?,?,?,?>> ref = new AtomicReference<>();
        Mono<ApiCollectorResponse> responseMono = addToCollectorMap(collectorHolder.monoResponse(),config,connector,creator,ref);
        if(ref.get()==null) return responseMono;
        return ref.get().validateAndSet(responseMono
                ,collectorHolder.getApiCallHolder().lastApiCall()
                ,r->Utils.setMessageAndData(Mono.fromSupplier(()->collectorHolder.addAndStartInfluxCollector(ref.get()))
                        ,"Metric Collector Started")
        );
    }

    public static Mono<JsonNode> getCollectorById(int collectorId){
        return Optional.ofNullable(METRIC_COLLECTOR_MAP.get(collectorId)).map(c->c.responseMono().map(Utils::valueToTree)).orElseGet(()-> Mono.just(Utils.getError("MetricService","Metric collector not found by id: "+collectorId)));
    }

}
