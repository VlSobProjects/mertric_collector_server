package ru.iteco.nt.metric_collector_server.influx.model.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.iteco.nt.metric_collector_server.DataResponse;
import ru.iteco.nt.metric_collector_server.influx.model.settings.WriterConfig;

@Getter
@SuperBuilder
public abstract class WriterResponse<S extends WriterConfig> extends DataResponse<S> {
    private final boolean success;
    private final int queueSize;
    private final boolean writing;
    private final int lastDataSize;
    private final int maxDataSize;
    private int maxQueueSize;
}
