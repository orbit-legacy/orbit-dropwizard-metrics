package cloud.orbit.actors.extensions.metrics.dropwizard;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.MessageDefinitions;

public class InstrumentedMessageSerializer implements MessageSerializer {
    private final MetricRegistry metricRegistry;
    private final MessageSerializer messageSerializer;
    private Map<Integer, Histogram> serializeMetrics;
    
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
    public Message deserializeMessage(BasicRuntime runtime, InputStream inputStream) throws Exception
    {
        return messageSerializer.deserializeMessage(runtime, inputStream);
    }

    @Override
    public void serializeMessage(BasicRuntime runtime, OutputStream out, Message message) throws Exception
    {
        messageSerializer.serializeMessage(runtime, out, message);
        
        Histogram serializeHistogram = serializeMetrics.get(message.getMessageType());
        if (serializeHistogram == null)
        {
            return;
        }
        
        if (out instanceof ByteArrayOutputStream)
        {
            ByteArrayOutputStream outByteArray = (ByteArrayOutputStream) out;
            serializeHistogram.update(outByteArray.size());
        }
    }
}
