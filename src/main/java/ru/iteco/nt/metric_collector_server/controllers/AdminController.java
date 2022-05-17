package ru.iteco.nt.metric_collector_server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.MetricService;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.influx.model.responses.WriterResponse;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ApiCollectorService apiCollectorService;

    @Operation(summary = "stop all metric writes and collectors")
    @RequestMapping(method = RequestMethod.GET,value = "/stopAllMetric")
    private Mono<Void> stopAllMetricAll(){
        return MetricService.stopAll();
    }

    @Operation(summary = "get all metric writes")
    @GetMapping("/getAllMetricWriters")
    private Mono<List<WriterResponse<?>>> getAllWriters(){
        return MetricService.getAllWriters();
    }

    @Operation(summary = "get all metric collectors")
    @GetMapping("/getAllMetricCollectors")
    private Mono<List<DataResponse<?>>> getAllCollectors(){
        return MetricService.getAllCollectors();
    }



}
