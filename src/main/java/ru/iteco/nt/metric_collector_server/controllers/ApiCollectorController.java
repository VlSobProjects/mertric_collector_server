package ru.iteco.nt.metric_collector_server.controllers;


import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.holders.DataCollector;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.influx.InfluxMetricCollectorGroup;
import ru.iteco.nt.metric_collector_server.influx.InfluxMetricService;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorConfig;
import ru.iteco.nt.metric_collector_server.influx.model.settings.InfluxMetricCollectorGroupConfig;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/collector")
public class ApiCollectorController {

    private final ApiCollectorService apiCollectorService;
    private final InfluxMetricService influxMetricService;

    @Operation(summary = "start collecting data from api with delay")
    @GetMapping("/startCollector/{collectorId}")
    private Mono<ApiCollectorResponse> startCollectorById(@PathVariable int collectorId){
        return apiCollectorService.startCollectorById(collectorId);
    }

    @Operation(summary = "stop collecting data from api by id")
    @GetMapping("/stopCollector/{collectorId}")
    private Mono<ApiCollectorResponse> stopCollectorById(@PathVariable int collectorId){
        return apiCollectorService.stopCollectorById(collectorId);
    }

    @Operation(summary = "delete ApiCollector by collector id")
    @RequestMapping(method = RequestMethod.DELETE,value = "/deleteCollector/{collectorId}")
    private Mono<ApiCallResponse> deleteCollectorById(@PathVariable int collectorId){
        return apiCollectorService.deleteCollectorById(collectorId);
    }

    @Operation(summary = "stream data from collector by id with delay in seconds")
    @GetMapping(value = "/data/{collectorId}",produces = MediaType.APPLICATION_NDJSON_VALUE)
    private Flux<DataCollector.ApiData> getCollectorData(@PathVariable int collectorId, @RequestParam long periodSeconds){
        return apiCollectorService.getCollectorData(collectorId,periodSeconds);
    }

}
