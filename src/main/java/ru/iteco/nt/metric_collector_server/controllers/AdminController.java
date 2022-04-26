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
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCollectorResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCall;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClient;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollector;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ApiCollectorService apiCollectorService;

    @Operation(summary = "Return all Api clients")
    @GetMapping("/client/all")
    private Mono<List<ApiClientResponse>> getAll(){
        return apiCollectorService.getAll();
    }

    @Operation(summary = "Return Api client  by Id")
    @GetMapping("/client/{clientId}")
    private Mono<ApiClientResponse> getclient(@PathVariable int clientId){
        return apiCollectorService.getApiClientById(clientId);
    }
    @Operation(summary = "Add Api client")
    @PostMapping(value = "/client/add")
    private Mono<ApiClientResponse> addClient(@RequestBody ApiClient client){
        return apiCollectorService.setApiClient(client);
    }

    @Operation(summary = "Add Api call to client",description = "ApiCall must have clientId with Id of existing Api client")
    @PostMapping("/call/add")
    private Mono<ApiCallResponse> addCall(@RequestBody ApiCall apiCall){
        return apiCollectorService.setApiCall(apiCall);
    }

    @Operation(summary = "Check api call and return data",
            description = "if api call fail and checkPeriod is set up, " +
                    "api call will be checking with customized period until api call ended in success." +
                    "If ApiCollector was set up for this ApiCall, after checking ended in success, ApiCollector will start too" +
                    "Wile ApiCall checking ApiCallResponse field - checking will return true"
    )
    @GetMapping("/call/{apiCallId}/check")
    private Mono<ApiCallResponse> callCheck(@PathVariable int apiCallId){
        return apiCollectorService.checkApiClientById(apiCallId);
    }

    @Operation(summary = "Return api call by Id")
    @GetMapping("/call/{apiCallId}/get")
    private Mono<ApiCallResponse> callGet(@PathVariable int apiCallId){
        return apiCollectorService.getApiCallById(apiCallId);
    }

    @Operation(summary = "Delete api client and all api call and api collectors by client Id and return list of existing api client")
    @RequestMapping(method = RequestMethod.DELETE,value = "/client/{clientId}")
    private Mono<List<ApiClientResponse>> deleteClient(@PathVariable int clientId){
        return apiCollectorService.deleteClientById(clientId);
    }

    @Operation(summary = "Delete api call and api collector by api call Id and return ApiClientResponse of connecting with api call client")
    @RequestMapping(method = RequestMethod.DELETE,value = "/call/{apiCallId}")
    private Mono<ApiClientResponse> deleteCall(@PathVariable int apiCallId){
        return apiCollectorService.deleteCallById(apiCallId);
    }

    @Operation(summary = "Manually stop checking api call",description = "checking will stop automatically if ended in success")
    @GetMapping("/call/{apiCallId}/stopCheck")
    private Mono<ApiCallResponse> stopCheckApi(@PathVariable int apiCallId){
        return apiCollectorService.stopCheckApi(apiCallId);
    }

    @Operation(summary = "set up ApiCollector -  collecting data from api with delay")
    @PostMapping(value = "/call/{apiCallId}/setCollector")
    private Mono<ApiCallResponse> setCollectorById(@PathVariable int apiCallId, @RequestBody ApiCollector apiCollector){
        return apiCollectorService.setCollectorById(apiCallId,apiCollector);
    }

    @Operation(summary = "start collecting data from api with delay")
    @GetMapping("/collector/{collectorId}/startCollector")
    private Mono<ApiCollectorResponse> startCollectorById(@PathVariable int collectorId){
        return apiCollectorService.startCollectorById(collectorId);
    }

    @Operation(summary = "stop collecting data from api by id of api call")
    @GetMapping("/call/{apiCallId}/stopCollector")
    private Mono<ApiCallResponse> stopCollectorByCallId(@PathVariable int apiCallId){
        return apiCollectorService.stopCollectorByCallId(apiCallId);
    }

    @Operation(summary = "delete ApiCollector from api call by api call id")
    @RequestMapping(method = RequestMethod.DELETE,value = "/call/{apiCallId}/deleteCollector")
    private Mono<ApiCallResponse> deleteCollectorByCallId(@PathVariable int apiCallId){
        return apiCollectorService.deleteCollectorByCallId(apiCallId);
    }



    @Operation(summary = "stop collecting data from api by id")
    @GetMapping("/collector/{collectorId}/stopCollector")
    private Mono<ApiCollectorResponse> stopCollectorById(@PathVariable int collectorId){
        return apiCollectorService.stopCollectorById(collectorId);
    }

    @Operation(summary = "delete ApiCollector by collector id")
    @RequestMapping(method = RequestMethod.DELETE,value = "/collector/{collectorId}/deleteCollector")
    private Mono<ApiCallResponse> deleteCollectorById(@PathVariable int collectorId){
        return apiCollectorService.deleteCollectorById(collectorId);
    }

    @Operation(summary = "delete all client, stop and delete all api cal and collectors")
    @RequestMapping(method = RequestMethod.DELETE,value = "/deleteAll")
    private Mono<Void> deleteAll(){
        return apiCollectorService.deleteAll();
    }


    @Operation(summary = "stream data from collector by id with delay in seconds")
    @GetMapping(value = "/collector/{collectorId}/data",produces = MediaType.APPLICATION_NDJSON_VALUE)
    private Flux<DataCollector.ApiData> getCollectorData(@PathVariable int collectorId, @RequestParam long periodSeconds){
        return apiCollectorService.getCollectorData(collectorId,periodSeconds);
    }

}
