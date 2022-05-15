package ru.iteco.nt.metric_collector_server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.influx.InfluxMetricService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxDbConnectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorGroupResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.InfluxMetricCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxDBConnectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/influx")
public class InfluxController {

    private final InfluxMetricService influxMetricService;

    @Operation(summary = "set up Influx data base connection")
    @PostMapping("/connection")
    private Mono<InfluxDbConnectorResponse> addConnection(@RequestBody InfluxDBConnectorConfig config){
        return influxMetricService.addConnector(config);
    }

    @Operation(summary = "connector start write to influx data base by id")
    @GetMapping("/connection/start/{connectorId}")
    private Mono<InfluxDbConnectorResponse> startWrite(@PathVariable int connectorId){
        return influxMetricService.startConnectorById(connectorId);
    }

    @Operation(summary = "connector stop write to influx data base by id")
    @GetMapping("/connection/stop/{connectorId}")
    private Mono<InfluxDbConnectorResponse> stopWrite(@PathVariable int connectorId){
        return influxMetricService.stopConnectorById(connectorId);
    }

    @Operation(summary = "get all influx database connectors")
    @GetMapping("/connection")
    private Mono<List<InfluxDbConnectorResponse>> getConnectors(){
        return influxMetricService.getAllDbConnectors();
    }

    @Operation(summary = "get influx database connector by id")
    @GetMapping("/connection/{connectorId}")
    private Mono<InfluxDbConnectorResponse> getConnector(@PathVariable int connectorId){
        return influxMetricService.getDbConnectorById(connectorId);
    }

    @Operation(summary = "add influx database collector to group (influxDbId and apiCollector not require, groupId - existing group)")
    @PostMapping("/collector/addToGroup/{groupId}")
    private Mono<InfluxMetricCollectorGroupResponse> addCollectorToGroup(@PathVariable int groupId, @RequestBody InfluxMetricCollectorConfig config){
        return influxMetricService.addCollectorToGroup(groupId,config);
    }
    @Operation(summary = "stop metric collector group by id")
    @GetMapping("/collector/stopGroup/{groupId}")
    public Mono<InfluxMetricCollectorGroupResponse> stopGroupById(@PathVariable int groupId){
        return influxMetricService.stopGroupById(groupId);
    }

    @Operation(summary = "stop metric collector by id")
    @GetMapping("/collector/stopSingle/{collectorId}")
    public Mono<InfluxMetricCollectorResponse> stopSingleCollectorById( @PathVariable int collectorId){
        return influxMetricService.stopSingleCollectorById(collectorId);
    }



}
