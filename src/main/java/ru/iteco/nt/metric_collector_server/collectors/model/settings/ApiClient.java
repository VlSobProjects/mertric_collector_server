package ru.iteco.nt.metric_collector_server.collectors.model.settings;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiClient {
    private boolean https;
    private String baseUrl;
    private String name;
    @Singular
    private  Map<String,String> headers;
    @Singular
    private Map<String, List<String>> cookies;
    @Builder.Default
    private int wrightReadTimeout = 0;
    @Builder.Default
    private int connectionTimeout = 0;
    @Builder.Default
    private int maxInMemorySize = 0;

    public WebClient getClient(WebClient.Builder builder){
        HttpClient httpClient = HttpClient.create();
        if(https){
            httpClient.secure(t->t.sslContext(getSslContext()));
        } else httpClient.noSSL();
        if(connectionTimeout>0){
            httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,connectionTimeout);
        }
        if(wrightReadTimeout>0){
            httpClient.doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(wrightReadTimeout));
                connection.addHandlerLast(new WriteTimeoutHandler(wrightReadTimeout));
            });
        }
        if(maxInMemorySize>0){
            builder.codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(maxInMemorySize));
        }
        if(headers!=null)
            headers.forEach((key,value)->builder.defaultHeaders(httpHeaders -> httpHeaders.add(key,value)));
        if(cookies!=null)
            cookies.forEach((key,list)->builder.defaultCookie(key,list.toArray(new String[0])));
        return builder.baseUrl(baseUrl).build();
    }

    @SneakyThrows
    private static SslContext getSslContext(){
        return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }

}
