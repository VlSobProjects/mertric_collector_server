package ru.iteco.nt.metric_collector_server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Api Data Collector", version = "1.0", description = "Documentation APIs v1.0"))
@SpringBootApplication
public class MetricCollectorApp {
    public static void main(String[] args) {
        SpringApplication.run(MetricCollectorApp.class,args);
    }
}
