package ru.iteco.nt.metric_collector_server;

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
}
