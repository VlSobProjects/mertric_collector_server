package ru.iteco.nt.metric_collector_server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Setter
@Getter
@EqualsAndHashCode
public abstract class MetricConfig {
    private Integer apiCollectorId;
    private Integer writerId;

    @JsonIgnore
    public abstract JsonNode shortVersion();
}
