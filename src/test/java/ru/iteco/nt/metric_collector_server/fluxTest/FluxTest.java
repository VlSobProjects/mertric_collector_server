package ru.iteco.nt.metric_collector_server.fluxTest;


import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FluxTest {

    Disposable d1 = null;

    @Test
    public void testFlux() throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        Flux<String> flux = Mono.fromSupplier(()->"String_"+count.getAndIncrement())
                .delayElement(Duration.ofSeconds(3))
                .repeat().flatMap(s->{
                    if(s.contains("0")){
                        d1.dispose();
                        return Mono.empty();
                    } else return Mono.just(s);
                }).share();
        d1 = flux.subscribe(s->System.out.println("d1 "+s));
        TimeUnit.SECONDS.sleep(5000);
        Disposable d2 = flux.subscribe(s->System.out.println("d2 "+s));
        Disposable d3 = flux.subscribe(s->System.out.println("d3 "+s));
        TimeUnit.SECONDS.sleep(10000);
//        Instant time = Instant.now();
//        while (Duration.between(time,Instant.now()).toMillis()<10000){
//
//        }
    }
}
