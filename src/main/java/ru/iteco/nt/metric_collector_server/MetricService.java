package ru.iteco.nt.metric_collector_server;

import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.*;
import ru.iteco.nt.metric_collector_server.influx.model.settings.WriterConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    protected abstract C getCollector(SC config);
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
        return addCollector(config, this::createAndAddSingle);
    }

    public Mono<ApiCollectorResponse> addGroupCollector(SG config){
        return addCollector(config,this::createAndAddGroup);
    }

    public Mono<RG> addCollectorToGroup(int groupId, SC config){
        return getCollectorGroupFromMap(groupId)
                .map(gr->Mono.fromSupplier(()->gr.addMetricCollector(getCollector(config))))
                .orElseGet(()->getErrorGroupCollector("MetricCollectorGroup not found by id: "+groupId,config));
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

    private boolean isConnectorExist(SW config){
        return METRIC_WRITER_MAP.values().stream().filter(writerClass::isInstance).anyMatch(c->c.isSameConfig(config));
    }

    private Optional<W> getWriterFromMap(int connectorId){
        return getByIdFormMap(connectorId,writerClass,METRIC_WRITER_MAP);
    }

    private Optional<G> getCollectorGroupFromMap(int collectorId){
        return getByIdFormMap(collectorId,collectorGroupClass,METRIC_COLLECTOR_MAP);
    }

    private Optional<C> getCollectorFromMap(int collectorId){
        return getByIdFormMap(collectorId,collectorClass,METRIC_COLLECTOR_MAP);
    }

    @SuppressWarnings("unchecked")
    private static <V,T extends V> Optional<T> getByIdFormMap(int id,Class<T> tClass,Map<Integer,V> map) {
        return Optional.ofNullable(map.get(id)).map(w->tClass.isInstance(w) ? (T)w : null);
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

    private C createAndAddSingle(SC config, W writer){
        C collector = getCollector(config,writer);
        METRIC_COLLECTOR_MAP.put(collector.getId(),collector);
        return collector;
    }

    private <T extends MetricConfig> Mono<ApiCollectorResponse> addCollector(T config, BiFunction<T,W, MetricCollector<?,?,?,?>> creator){
        if(config.getApiCollectorId()==null)
            return ApiCollectorResponse.factoryError("MetricService.addCollector","not set apiCollectorId",config);
        if(config.getWriterId()==null)
            return ApiCollectorResponse.factoryError("MetricService.addCollector","not set MetricWriter for MetricCollector",config);
        W connector = getWriterFromMap(config.getWriterId()).orElse(null);
        if(connector==null)
            return ApiCollectorResponse.factoryError("MetricService.addCollector",String.format("not found MetricWriter by Id: %s, set up MetricWriter first",config.getWriterId()),config);
        return apiCollectorService
                .getApiCollectorById(config.getApiCollectorId())
                .map(holder->Mono.fromSupplier(()->holder.addAndStartInfluxCollector(creator.apply(config,connector))))
                .orElseGet(()->ApiCollectorResponse.factoryError("MetricService.addCollector",String.format("not found ApiCollector by Id: %s",config.getApiCollectorId()),config));
    }

    private G createAndAddGroup(SG config,W connector){
        G groupCollector = getGroupCollector(config,connector);
        METRIC_COLLECTOR_MAP.put(groupCollector.getId(),groupCollector);
        return groupCollector;
    }



}
