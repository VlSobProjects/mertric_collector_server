package ru.iteco.nt.metric_collector_server.influx;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxDbConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class InfluxMetricService {

    private final ApiCollectorService apiCollectorService;

    private final Map<Integer,InfluxDbConnector> influxDbConnectorMap = new ConcurrentHashMap<>();
    private final Map<Integer,InfluxMetricCollector> singleMetricCollectors = new ConcurrentHashMap<>();
    private final Map<Integer,InfluxMetricCollectorGroup> groupMetricCollectors = new ConcurrentHashMap<>();




    public Mono<InfluxDbConnectorResponse> addConnector(InfluxDBConnectorConfig config){
        return isConnectorExist(config) ? InfluxDbConnectorResponse
                .factoryError("InfluxMetricService.addConnector","InfluxDBConnector with same config exists",getResponseByConfig(config)) :
                Mono.fromSupplier(()->{
                    InfluxDbConnector connector = new InfluxDbConnector(config);
                    influxDbConnectorMap.put(connector.getId(),connector);
                    return connector.response();
                });
    }

    public Mono<InfluxDbConnectorResponse> startConnectorById(int connectorId){
        return Optional.ofNullable(influxDbConnectorMap.get(connectorId))
                .map(InfluxDbConnector::startWriteToInflux)
                .orElseGet(()->notFoundById(connectorId));
    }

    public Mono<InfluxDbConnectorResponse> stopConnectorById(int connectorId){
        return Optional.ofNullable(influxDbConnectorMap.get(connectorId))
                .map(InfluxDbConnector::stopWriteToInflux)
                .orElseGet(()->notFoundById(connectorId));
    }

    public Mono<InfluxDbConnectorResponse> getDbConnectorById(int connectorId){
        return Optional.ofNullable(influxDbConnectorMap.get(connectorId))
                .map(InfluxDbConnector::responseMono)
                .orElseGet(()->notFoundById(connectorId));
    }

    public Mono<List<InfluxDbConnectorResponse>> getAllDbConnectors(){
        return Mono.fromSupplier(()->influxDbConnectorMap.values().stream().map(InfluxDbConnector::response).collect(Collectors.toList()));
    }

    public Mono<ApiCollectorResponse> addSingleCollector(InfluxMetricCollectorConfig config){
        return addCollector(config, this::createAndAddSingle);
    }

    public Mono<ApiCollectorResponse> addGroupCollector(InfluxMetricCollectorGroupConfig config){
        return addCollector(config,this::createAndAddGroup);
    }

    public Mono<InfluxMetricCollectorGroupResponse> addCollectorToGroup(int groupId, InfluxMetricCollectorConfig config){
        return Optional.ofNullable(groupMetricCollectors.get(groupId))
                .map(gr->Mono.fromSupplier(()->gr.addInfluxMetricCollector(config)))
                .orElseGet(()->InfluxMetricCollectorGroupResponse.factoryError("InfluxMetricService.addCollectorToGroup","can't add InfluxMetricCollector to group, InfluxMetricCollectorGroup not found by id: "+groupId,config));
    }

    public Mono<InfluxMetricCollectorGroupResponse> stopGroupById(int groupId){
        return Optional.ofNullable(groupMetricCollectors.get(groupId))
                .map(gr->Mono.fromSupplier(gr::stopCollecting))
                .orElseGet(()->InfluxMetricCollectorGroupResponse.factoryError("InfluxMetricService.stopGroupById","can't stop InfluxMetricCollectorGroup, InfluxMetricCollectorGroup not found by id: "+groupId));
    }

    public Mono<InfluxMetricCollectorResponse> stopSingleCollectorById(int collectorId){
        return Optional.ofNullable(singleMetricCollectors.get(collectorId))
                .map(c->Mono.fromSupplier(c::stopCollecting))
                .orElseGet(()->InfluxMetricCollectorResponse.factoryError("InfluxMetricService.stopSingleCollectorById","can't stop InfluxMetricCollector, InfluxMetricCollector not found by id: "+collectorId));

    }

    public Mono<Void> stopAndDeleteAll(){
        return Mono.fromRunnable(()->{
            Stream.of(singleMetricCollectors.values().stream(),groupMetricCollectors.values().stream())
                    .flatMap(Function.identity())
                    .forEach(InfluxCollector::stop);
            singleMetricCollectors.clear();
            groupMetricCollectors.clear();
            influxDbConnectorMap.values().forEach(InfluxDbConnector::stop);
            influxDbConnectorMap.clear();

        });
    }




    private <T extends InfluxMetricConfig> Mono<ApiCollectorResponse> addCollector(T config, BiFunction<T,InfluxDbConnector,InfluxCollector<?>> creator){
        if(config.getApiCollectorId()==null)
            return ApiCollectorResponse.factoryError("InfluxMetricService.addCollector","not set apiCollectorId",config);
        if(config.getInfluxDbId()==null)
            return ApiCollectorResponse.factoryError("InfluxMetricService.addCollector","not set influxDbId for single influxMetricCollector",config);
        InfluxDbConnector connector = influxDbConnectorMap.get(config.getInfluxDbId());
        if(connector==null)
            return ApiCollectorResponse.factoryError("InfluxMetricService.addCollector",String.format("not found InfluxDbConnector by Id: %s, set up InfluxDbConnector first",config.getInfluxDbId()),config);
        return apiCollectorService
                .getApiCollectorById(config.getApiCollectorId())
                .map(holder->Mono.fromSupplier(()->holder.addAndStartInfluxCollector(creator.apply(config,connector))))
                .orElseGet(()->ApiCollectorResponse.factoryError("InfluxMetricService.addCollector",String.format("not found ApiCollector by Id: %s, set up InfluxDbConnector first",config.getApiCollectorId()),config));
    }

    private InfluxMetricCollector createAndAddSingle(InfluxMetricCollectorConfig config,InfluxDbConnector connector){
        InfluxMetricCollector collector = new InfluxMetricCollector(config,connector);
        singleMetricCollectors.put(collector.getId(),collector);
        return collector;
    }

    private InfluxMetricCollectorGroup createAndAddGroup(InfluxMetricCollectorGroupConfig config,InfluxDbConnector connector){
        InfluxMetricCollectorGroup group = new InfluxMetricCollectorGroup(connector,config);
        groupMetricCollectors.put(group.getId(),group);
        return group;
    }

    private boolean isConnectorExist(InfluxDBConnectorConfig config){
        return influxDbConnectorMap.values().stream().anyMatch(c->c.isSameConfig(config));
    }
    private InfluxDbConnectorResponse getResponseByConfig(InfluxDBConnectorConfig config){
        return influxDbConnectorMap.values().stream().filter(c->c.isSameConfig(config)).findFirst().map(InfluxDbConnector::response).orElse(null);
    }

    private Mono<InfluxDbConnectorResponse> notFoundById(int id){
        return InfluxDbConnectorResponse.factoryError("InfluxMetricService.getDbConnectorById","InfluxDbConnector not found by id: "+id);
    }
}
