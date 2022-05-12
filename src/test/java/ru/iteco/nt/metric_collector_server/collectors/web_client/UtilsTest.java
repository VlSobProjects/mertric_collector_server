package ru.iteco.nt.metric_collector_server.collectors.web_client;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import ru.iteco.nt.metric_collector_server.collectors.model.responses.ApiCallResponse;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.util.Arrays;

class UtilsTest {



    @Test
    void getFromJsonNode() {
        ApiCallResponse apiCallResponse1 = ApiCallResponse.factoryError("testErrorSource1","testErrorMessage1").block();
        ApiCallResponse apiCallResponse2 = ApiCallResponse.factoryError("testErrorSource2","testErrorMessage2").block();
        ApiCallResponse apiCallResponse3 = ApiCallResponse.factoryError("testErrorSource3","testErrorMessage3").block();

        ArrayNode arr = Utils.toArrayNode(Arrays.asList(apiCallResponse1,apiCallResponse3,apiCallResponse2));


        System.out.println(Utils.getFromJsonNode(arr,"/data/error/errorMessage").toPrettyString());
    }
}