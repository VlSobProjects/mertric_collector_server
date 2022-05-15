package ru.iteco.nt.metric_collector_server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiClientResponse;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCall;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCollector;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/call")
public class ApiCallController {

    private final ApiCollectorService apiCollectorService;

    @Operation(summary = "Add Api call to client",description = "ApiCall must have clientId with Id of existing Api client")
    @PostMapping("/add")
    private Mono<ApiCallResponse> addCall(@RequestBody ApiCall apiCall){
        return apiCollectorService.setApiCall(apiCall);
    }

    @Operation(summary = "Check api call and return data",
            description = "if api call fail and checkPeriod is set up, " +
                    "api call will be checking with customized period until api call ended in success." +
                    "If ApiCollector was set up for this ApiCall, after checking ended in success, ApiCollector will start too" +
                    "Wile ApiCall checking ApiCallResponse field - checking will return true"
    )
    @GetMapping("/check/{apiCallId}")
    private Mono<ApiCallResponse> callCheck(@PathVariable int apiCallId){
        return apiCollectorService.checkApiClientById(apiCallId);
    }

    @Operation(summary = "Return api call by Id")
    @GetMapping("/{apiCallId}")
    private Mono<ApiCallResponse> callGet(@PathVariable int apiCallId){
        return apiCollectorService.getApiCallById(apiCallId);
    }

    @Operation(summary = "Delete api call and api collector by api call Id and return ApiClientResponse of connecting with api call client")
    @RequestMapping(method = RequestMethod.DELETE,value = "/call/{apiCallId}")
    private Mono<ApiClientResponse> deleteCall(@PathVariable int apiCallId){
        return apiCollectorService.deleteCallById(apiCallId);
    }

    @Operation(summary = "Manually stop checking api call",description = "checking will stop automatically if ended in success")
    @GetMapping("/stopCheck/{apiCallId}")
    private Mono<ApiCallResponse> stopCheckApi(@PathVariable int apiCallId){
        return apiCollectorService.stopCheckApi(apiCallId);
    }

    @Operation(summary = "set up ApiCollector -  collecting data from api with delay")
    @PostMapping(value = "/setCollector/{apiCallId}")
    private Mono<ApiCallResponse> setCollectorById(@PathVariable int apiCallId, @RequestBody ApiCollector apiCollector){
        return apiCollectorService.setCollectorById(apiCallId,apiCollector);
    }

    @Operation(summary = "stop collecting data from api by id of api call")
    @GetMapping("/stopCollector/{apiCallId}")
    private Mono<ApiCallResponse> stopCollectorByCallId(@PathVariable int apiCallId){
        return apiCollectorService.stopCollectorByCallId(apiCallId);
    }

    @Operation(summary = "Delete ApiCollector from api call by api call id")
    @RequestMapping(method = RequestMethod.DELETE,value = "/deleteCollector/{apiCallId}")
    private Mono<ApiCallResponse> deleteCollectorByCallId(@PathVariable int apiCallId){
        return apiCollectorService.deleteCollectorByCallId(apiCallId);
    }
}
