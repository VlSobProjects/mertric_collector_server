package ru.iteco.nt.metric_collector_server.controllers;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.Disposable;
import ru.iteco.nt.metric_collector_server.collectors.ApiCollectorService;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiCall;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClient;
import ru.iteco.nt.metric_collector_server.collectors.web_client.Utils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureWebTestClient(timeout = "PT10S")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminControllerTest {
/*
    @Autowired
    WebTestClient webTestClient;

    static ApiCollectorService.ApiClientHolder clientHolder = null;
    static ApiCollectorService.ApiCallHolder apiCallHolder = null;


    @Test
    @Order(1)
    void clientAdd() {
        webTestClient
                .post()
                .uri("/admin/client/add?name=stub")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiClient.builder()
                        .baseUrl("http://10.55.218.98:8082")
                        .https(false)
                        .build()
                ).exchange().
                expectStatus().
                is2xxSuccessful().
                expectBody(ApiCollectorService.ApiClientHolder.class).
                value(c->clientHolder = c)
        ;
        assertNotNull(clientHolder);
        System.out.println(Utils.valueToTree(clientHolder));
    }

    @Test
    @Order(2)
    void testCall(){
        webTestClient
                .post()
                .uri("/admin/call/add")
                .bodyValue(ApiCall
                                .builder()
                                .uri("/healthCheck")
                                .clientId(clientHolder.getId())
                                .method("GET")
                                .name("Заглушка healthCheck")
                                .build()
                ).exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ApiCollectorService.ApiCallHolder.class).value(c->apiCallHolder=c);
        assertNotNull(apiCallHolder);
        System.out.println(Utils.valueToTree(apiCallHolder));

//        Disposable d = webClient
//                .post()
//                .uri("/admin/client/add?name=stub")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(ApiClient.builder()
//                        .baseUrl("http://10.55.226.37:8082")
//                        .https(false)
//                        .build())
//                .exchangeToMono(r->r.bodyToMono(JsonNode.class))
//                .doOnNext(System.out::println)
//                .flatMap(client->
//                        webClient
//                                .post()
//                                .uri("/call/add")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .bodyValue(ApiCall
//                                        .builder()
//                                        .uri("/healthCheck")
//                                        .clientId(client.get("id").asInt())
//                                        .method("GET")
//                                        .name("Заглушка healthCheck")
//                                        .build()
//                                ).exchangeToMono(r->r.bodyToMono(JsonNode.class))
//                        ).doOnNext(System.out::println)
//                .flatMapMany(j->webClient.get()
//                        .uri(String.format("/collector?callId=%s&periodMillis=5000",j.get("id")))
//                        .exchangeToFlux(c->c.bodyToFlux(ApiCollectorService.ApiCollector.class)))
//                .subscribe(c-> System.out.println(Utils.valueToTree(c).toPrettyString()))
//        ;
//
//        LocalDateTime time = LocalDateTime.now();
//        while (time.isBefore(time.plusSeconds(60)) && !d.isDisposed()){
//
//        }
//        d.dispose();


    }

    @Test
    @Order(3)
    void testCollector() {
        Disposable d = webTestClient
                .get()
                .uri(String.format("/admin/collector?callId=%s&periodMillis=5000",apiCallHolder.getId()))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(ApiCollectorService.ApiCollector.class)
                .getResponseBody()
                .subscribe(c->System.out.println(Utils.valueToTree(c).toPrettyString()));
        LocalDateTime time = LocalDateTime.now().plus(Duration.ofSeconds(15));
        while (time.isAfter(LocalDateTime.now()) && !d.isDisposed()){

        }
        d.dispose();
    }
}
}

 */
}