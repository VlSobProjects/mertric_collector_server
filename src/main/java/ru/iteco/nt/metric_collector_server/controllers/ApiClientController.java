package ru.iteco.nt.metric_collector_server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClient;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/client")
public class ApiClientController {
    private final ApiCollectorService apiCollectorService;

    @Operation(summary = "Return all Api clients")
    @GetMapping("/all")
    private Mono<List<ApiClientResponse>> getAll(){
        return apiCollectorService.getAll();
    }

    @Operation(summary = "Return Api client  by Id")
    @GetMapping("/{clientId}")
    private Mono<ApiClientResponse> getclient(@PathVariable int clientId){
        return apiCollectorService.getApiClientById(clientId);
    }
    @Operation(summary = "Add Api client")
    @PostMapping(value = "/add")
    private Mono<ApiClientResponse> addClient(@RequestBody ApiClient client){
        return apiCollectorService.setApiClient(client);
    }

    @Operation(summary = "Delete api client and all api call and api collectors by client Id and return list of existing api client")
    @RequestMapping(method = RequestMethod.DELETE,value = "/{clientId}")
    private Mono<List<ApiClientResponse>> deleteClient(@PathVariable int clientId){
        return apiCollectorService.deleteClientById(clientId);
    }
}
