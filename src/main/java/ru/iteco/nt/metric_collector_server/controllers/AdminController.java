package ru.iteco.nt.metric_collector_server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.influx.InfluxMetricService;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ApiCollectorService apiCollectorService;
    private final InfluxMetricService influxMetricService;


    @Operation(summary = "stop and delete all influx connectors, metric collectors, api collectors,api calls ,api clients")
    @RequestMapping(method = RequestMethod.DELETE,value = "/deleteAll")
    private Mono<Void> deleteAll(){
        return influxMetricService.stopAndDeleteAll().then(apiCollectorService.deleteAll());
    }



}
