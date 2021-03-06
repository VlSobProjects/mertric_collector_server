package ru.iteco.nt.metric_collector_server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.MetricService;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;
import ru.iteco.nt.metric_collector_server.influx.model.responses.WriterResponse;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {


    @Operation(summary = "stop all metric writes and metric collectors")
    @RequestMapping(method = RequestMethod.GET,value = "/stopAllMetric")
    private Mono<Void> stopAllMetricAll(){
        return MetricService.stopAll();
    }

    @Operation(summary = "get all metric writes")
    @GetMapping("/allMetricWriters")
    private Mono<List<WriterResponse<?>>> getAllWriters(){
        return MetricService.getAllWriters();
    }

    @Operation(summary = "get all metric collectors")
    @GetMapping("/allMetricCollectors")
    private Mono<List<DataResponse<?>>> getAllCollectors(){
        return MetricService.getAllCollectors();
    }

    @Operation(summary = "stop and delete all metric writes and metric collectors")
    @RequestMapping(method = RequestMethod.DELETE,value = "/deleteAllMetric")
    private Mono<Void> deleteAllMetricAnControllers(){
        return MetricService.stopAndClearAll();
    }

    @Operation(summary = "stop and delete all api clients, api calls, api collectors, metric writes and metric collectors")
    @RequestMapping(method = RequestMethod.DELETE,value = "/deleteAll")
    private Mono<Void> clearAll(){
        return deleteAllMetricAnControllers().then(ApiCollectorService.deleteAll());
    }

    @Operation(summary = "get Metric Collector by Id")
    @GetMapping ("/collector/{collectorId}")
    private Mono<JsonNode> getCollectorById(@PathVariable int collectorId){
        return MetricService.getCollectorById(collectorId);
    }

    @Operation(summary = "get all Api Clients")
    @GetMapping("/allApiClients")
    private Mono<List<ApiClientResponse>> getAllApiClients(){
        return ApiCollectorService.getAllClients();
    }

    @Operation(summary = "remove all validated with api data metric collectors not pass validation")
    @RequestMapping(method = RequestMethod.DELETE,value = "/failMetricCollectorsRemove")
    private Mono<List<DataResponse<?>>> removeFailMetricCollectors(){
        return MetricService.removeValidationFailCollectors();
    }

    @Operation(summary = "get all validated with api data metric collectors not pass validation")
    @GetMapping( "/failMetricCollectorsRemove")
    private Mono<List<DataResponse<?>>> getFailMetricCollectors(){
        return MetricService.getValidationFailCollectors();
    }
}
