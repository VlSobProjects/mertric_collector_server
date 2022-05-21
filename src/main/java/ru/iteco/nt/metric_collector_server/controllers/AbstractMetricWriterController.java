package ru.iteco.nt.metric_collector_server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.MetricConfig;
import ru.iteco.nt.metric_collector_server.MetricService;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.*;
import ru.iteco.nt.metric_collector_server.influx.model.settings.WriterConfig;

import java.util.List;

@RequiredArgsConstructor
public abstract class AbstractMetricWriterController <
        SW extends WriterConfig
        ,RW extends WriterResponse<SW>
        ,SC extends MetricConfig
        ,RC extends DataResponse<?> & ResponseWithMessage<RC>
        ,SG extends MetricConfig
        ,RG extends DataResponse<SG> & ResponseWithMessage<RG>
        ,SE extends MetricService<?,SW,RW,?,SC,RC,?,SG,RG,?>
        > {

    @Getter
    private final SE metricService;

    @Operation(summary = "set up metric writer")
    @PostMapping("/connection")
    private Mono<RW> addConnection(@RequestBody SW config){
        return metricService.addConnector(config);
    }

    @Operation(summary = "connector start write to influx data base by id")
    @GetMapping("/connection/start/{connectorId}")
    private Mono<RW> startWrite(@PathVariable int connectorId){
        return metricService.startConnectorById(connectorId);
    }

    @Operation(summary = "connector stop write to influx data base by id")
    @GetMapping("/connection/stop/{connectorId}")
    private Mono<RW> stopWrite(@PathVariable int connectorId){
        return metricService.stopConnectorById(connectorId);
    }

    @Operation(summary = "get all metric writers")
    @GetMapping("/connection")
    private Mono<List<RW>> getConnectors(){
        return metricService.getAllServiceWriters();
    }

    @Operation(summary = "get metric writer by id")
    @GetMapping("/connection/{connectorId}")
    private Mono<RW> getConnector(@PathVariable int connectorId){
        return metricService.getDbConnectorById(connectorId);
    }

    @Operation(summary = "add influx metric collector group (config require fields: (influxDbId - existing influxDbConnector id, apiCollectorId - exist apiCollector id))")
    @PostMapping("/collector/addGroup")
    private Mono<ApiCollectorResponse> addInfluxMetricCollectorGroup(@RequestBody SG config){
        return metricService.addGroupCollector(config);
    }

    @Operation(summary = "add influx metric collector (config require fields: (influxDbId - existing influxDbConnector id, apiCollectorId - exist apiCollector id))")
    @PostMapping("/collector/addCollector")
    private Mono<ApiCollectorResponse> addInfluxMetricCollector(@RequestBody SC config){
        return metricService.addSingleCollector(config);
    }

    @Operation(summary = "validate collector single collector by id")
    @PostMapping("/collector/validate/{collectorId}")
    private Mono<RC> validateCollector(@PathVariable int collectorId){
        return metricService.validateCollector(collectorId);
    }

    @Operation(summary = "validate collector group by id")
    @PostMapping("/collector/validate/{collectorGroupId}")
    private Mono<RG> validateCollectorGroup(@PathVariable int collectorGroupId){
        return metricService.validateGroup(collectorGroupId);
    }

    @Operation(summary = "add metric collector to group (influxDbId and apiCollector not require, groupId - existing group)")
    @PostMapping("/collector/addToGroup/{groupId}")
    private Mono<RG> addCollectorToGroup(@PathVariable int groupId, @RequestBody SC config){
        return metricService.addCollectorToGroup(groupId,config);
    }

    @Operation(summary = "stop metric collector group by id")
    @GetMapping("/collector/stopGroup/{groupId}")
    public Mono<RG> stopGroupById(@PathVariable int groupId){
        return metricService.stopGroupById(groupId);
    }

    @Operation(summary = "start metric collector group by id")
    @GetMapping("/collector/startGroup/{groupId}")
    public Mono<RG> startGroupById(@PathVariable int groupId){
        return metricService.startGroupById(groupId);
    }

    @Operation(summary = "stop metric collector by id")
    @GetMapping("/collector/stopSingle/{collectorId}")
    public Mono<RC> stopSingleCollectorById(@PathVariable int collectorId){
        return metricService.stopSingleCollectorById(collectorId);
    }

    @Operation(summary = "start metric collector by id")
    @GetMapping("/collector/startSingle/{collectorId}")
    public Mono<RC> startSingleCollectorById( @PathVariable int collectorId){
        return metricService.startCollectorById(collectorId);
    }


}
