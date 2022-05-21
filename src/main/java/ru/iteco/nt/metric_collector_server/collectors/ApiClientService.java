package ru.iteco.nt.metric_collector_server.collectors;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.iteco.nt.metric_collector_server.collectors.model.settings.ApiClientConfig;
import ru.iteco.nt.metric_collector_server.utils.Utils;

@RequiredArgsConstructor
@Service
public class ApiClientService {

    public WebClient getApiClientBuilder(ApiClientConfig apiClientConfig){
        return apiClientConfig.getClient(WebClient.builder());
    }

    public WebClient.Builder getBuilder(HttpClient httpClient){
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(Utils.logRequest())
                .filter(Utils.logResponse())
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    public WebClient.Builder getBuilder(){
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(Utils.logRequest())
                .filter(Utils.logResponse());
    }


    @SneakyThrows
    private static SslContext getSslContext(){
        return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }

}
