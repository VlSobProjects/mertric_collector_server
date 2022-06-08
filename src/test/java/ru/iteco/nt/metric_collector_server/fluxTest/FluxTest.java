package ru.iteco.nt.metric_collector_server.fluxTest;


import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.iteco.nt.metric_collector_server.utils.Utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

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


    @Test
    void delayTest() throws InterruptedException {
        Duration d = Duration.ofSeconds(3);
        Mono<String> mono = Mono.fromSupplier(()->"String_"+ LocalDateTime.now());
        //collector = apiCallHolder.getRequest().delayElement(Duration.ofMillis(apiCollectorConfig.getPeriodMillis())).repeat().filter(j->!j.has("error")).doOnNext(this::setData).share();

        Flux<String> flux1 = mono.delayElement(d).repeat().doOnNext(s->System.out.println("flux 1 "+s+" - "+LocalDateTime.now()));
        Flux<String> flux2 = Mono.delay(d).then(mono).repeat().doOnNext(s->System.out.println("flux 2 "+s+" - "+LocalDateTime.now()));

        Disposable d1 = flux1.subscribe();
        Disposable d2 = flux2.subscribe();

        TimeUnit.SECONDS.sleep(15);
        d1.dispose();
        d2.dispose();
    }

    @Test
    void testWebClient() throws InterruptedException {
        WebClient client = WebClient.create("http://localhost:8082/simple");
        Disposable d = Utils.getWithOnHttpErrorResponseSpec("test",client.get().uri("/testServerErrorNoBody").retrieve(), Retry.backoff(3,Duration.ofSeconds(1)))
                .subscribe(j->System.out.println("ResponseObject: "+j));


        while (!d.isDisposed())
            TimeUnit.SECONDS.sleep(1);
    }

    @Test
    void testFlux2() throws InterruptedException {
        Collector collector = new Collector(Duration.ofSeconds(2));
        collector.start();
        TimeUnit.SECONDS.sleep(9);
        collector.stop();

    }

    @Test
    void testFLUX3() throws InterruptedException {
        Disposable d1 = getMono().delayElement(Duration.ofSeconds(2)).repeat().subscribe(c->System.out.println("Test1 - "+c));
        Disposable d2 = Mono.fromSupplier(this::getMono).flatMap(Function.identity()).delayElement(Duration.ofSeconds(2)).repeat().subscribe(c->System.out.println("Test2 - "+c));

        TimeUnit.SECONDS.sleep(10);
        d1.dispose();
        d2.dispose();

    }

    private Mono<String> getMono(){
        String str = "testString_"+System.currentTimeMillis();
        return Mono.fromSupplier(()->str);
    }

    private static class Collector {
        private final Flux<String> collector;
        private final List<Subscriber> list = new ArrayList<>();
        private Disposable d;

        private Collector(Duration d) {
            Mono<String> mono = Mono.fromSupplier(()->"String_"+ LocalDateTime.now());
            this.collector = Mono.delay(d).then(mono).repeat().doOnNext(s->System.out.println("flux 2 "+s+" - "+LocalDateTime.now())).doOnNext(s->startSubscribers()).share();
        }

        void startSubscribers(){
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add(new Subscriber());
            list.forEach(s->s.start(collector));
        }

        void start(){
            if(d==null || d.isDisposed())
                d = collector.subscribe(s->System.out.println(LocalDateTime.now()+" Collector "+s));
        }
        void stop(){
            list.forEach(Subscriber::stop);
            if(d!=null)
                d.dispose();
        }

    }

    private static class Subscriber{
        private static final AtomicInteger ID_SOURCE = new AtomicInteger();
        private Disposable d;
        private final int id = ID_SOURCE.getAndIncrement();

        void start(Flux<String> stringFlux){
            if(d==null || d.isDisposed())
                d = stringFlux.subscribe(s->System.out.println(s+" from Subscriber "+id));
        }
        void stop(){
            if(d!=null && !d.isDisposed()){
                d.dispose();
            }
        }
    }
}
