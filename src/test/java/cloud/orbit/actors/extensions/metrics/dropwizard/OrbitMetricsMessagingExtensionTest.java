package cloud.orbit.actors.extensions.metrics.dropwizard;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.SortedMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.MessageDefinitions;

public class OrbitMetricsMessagingExtensionTest {
    private OrbitMetricsMessagingExtension extension;

    @Mock
    private HandlerContext context;
    
    private MetricRegistry metricRegistry;

    @Before
    public void before()
    {
        MockitoAnnotations.initMocks(this);
        
        metricRegistry = new MetricRegistry();
        extension = new OrbitMetricsMessagingExtension(metricRegistry);
    }

    @Test
    public void testWrite_HeaderAdded() throws Exception
    {
        Message message = new Message();
        extension.write(context, message);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).write(messageCaptor.capture());

        Message capturedMessage = messageCaptor.getValue();
        assertTrue(capturedMessage.getHeader("metrics-ts") != null);
    }

    @Test
    public void testOnRead_MetricsRecorded()
    {
        SortedMap<String, Timer> timers = metricRegistry.getTimers();
        Timer timer = timers.get("orbit.messaging[type:one_way_message,direction:inbound]");
        long count = timer.getCount();

        Message message = new Message();
        message.setMessageType(MessageDefinitions.ONE_WAY_MESSAGE);
        message.setHeader("metrics-ts", System.currentTimeMillis());

        extension.onRead(context, message);

        assertTrue(timer.getCount() - count == 1);
    }

    @Test
    public void testOnRead_MessageWithoutHeader_Ignored()
    {
        SortedMap<String, Timer> timers = metricRegistry.getTimers();
        Timer timer = timers.get("orbit.messaging[type:one_way_message,direction:inbound]");
        long count = timer.getCount();

        Message message = new Message();
        message.setMessageType(MessageDefinitions.ONE_WAY_MESSAGE);

        extension.onRead(context, message);

        assertTrue(timer.getCount() - count == 0);
    }
}
