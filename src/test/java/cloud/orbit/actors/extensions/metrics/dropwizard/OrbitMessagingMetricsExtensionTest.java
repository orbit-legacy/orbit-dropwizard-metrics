package cloud.orbit.actors.extensions.metrics.dropwizard;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.SortedMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.codahale.metrics.Timer;

import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.MessageDefinitions;

public class OrbitMessagingMetricsExtensionTest {
    private OrbitMessagingMetricsExtension extension;
    private MetricsManager metricsManager;

    @Mock
    private HandlerContext context;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        metricsManager = MetricsManager.getInstance();

        extension = new OrbitMessagingMetricsExtension();
    }

    @Test
    public void testWrite_HeaderAdded() throws Exception {
        Message message = new Message();
        extension.write(context, message);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).write(messageCaptor.capture());

        Message capturedMessage = messageCaptor.getValue();
        assertTrue(capturedMessage.getHeader("metrics-ts") != null);
    }

    @Test
    public void testOnRead_MetricsRecorded() {
        SortedMap<String, Timer> timers = metricsManager.getRegistry()
                .getTimers();
        Timer timer = timers.get("orbit.messaging[type:one_way_message,direction:inbound]");
        long count = timer.getCount();

        Message message = new Message();
        message.setMessageType(MessageDefinitions.ONE_WAY_MESSAGE);
        message.setHeader("metrics-ts", System.currentTimeMillis());

        extension.onRead(context, message);

        assertTrue(timer.getCount() - count == 1);
    }

    @Test
    public void testOnRead_MessageWithoutHeader_Ignored() {
        SortedMap<String, Timer> timers = metricsManager.getRegistry()
                .getTimers();
        Timer timer = timers.get("orbit.messaging[type:one_way_message,direction:inbound]");
        long count = timer.getCount();

        Message message = new Message();
        message.setMessageType(MessageDefinitions.ONE_WAY_MESSAGE);

        extension.onRead(context, message);

        assertTrue(timer.getCount() - count == 0);
    }
}
