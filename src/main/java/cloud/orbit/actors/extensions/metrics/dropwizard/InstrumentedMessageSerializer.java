package cloud.orbit.actors.extensions.metrics.dropwizard;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.MessageDefinitions;

public class InstrumentedMessageSerializer implements MessageSerializer {
    private final MetricRegistry metricRegistry;
    private final MessageSerializer messageSerializer;
    private Map<Integer, Histogram> serializeMetrics = new ConcurrentHashMap<>();

    public InstrumentedMessageSerializer(MessageSerializer messageSerializer)
    {
        this(new MetricRegistry(), messageSerializer);
    }

    public InstrumentedMessageSerializer(MetricRegistry metricRegistry, MessageSerializer messageSerializer)
    {
        this.metricRegistry = metricRegistry;
        this.messageSerializer = messageSerializer;
        setupMetrics();
    }

    public MetricRegistry getMetricRegistry()
    {
        return metricRegistry;
    }

    private void setupMetrics()
    {
        String metricName = "orbit.messaging.size_in_bytes";
        serializeMetrics.put((int) MessageDefinitions.ONE_WAY_MESSAGE, metricRegistry.histogram(metricName + "[type:one_way_message]"));
        serializeMetrics.put((int) MessageDefinitions.REQUEST_MESSAGE, metricRegistry.histogram(metricName + "[type:request_message]"));
        serializeMetrics.put((int) MessageDefinitions.RESPONSE_ERROR, metricRegistry.histogram(metricName + "[type:response_error]"));
        serializeMetrics.put((int) MessageDefinitions.RESPONSE_OK, metricRegistry.histogram(metricName + "[type:response_ok]"));
        serializeMetrics.put((int) MessageDefinitions.RESPONSE_PROTOCOL_ERROR, metricRegistry.histogram(metricName + "[type:response_protocol_error]"));
    }        

    @Override
    public Message deserializeMessage(BasicRuntime runtime, byte[] payload) throws Exception {
        return messageSerializer.deserializeMessage(runtime, payload);
    }

    @Override
    public byte[] serializeMessage(BasicRuntime runtime, Message message) throws Exception {
        byte[] out = messageSerializer.serializeMessage(runtime, message);

        Histogram serializeHistogram = serializeMetrics.get(message.getMessageType());
        if (serializeHistogram != null)
        {
            serializeHistogram.update(out.length);
        }

        return out;
    }
}
