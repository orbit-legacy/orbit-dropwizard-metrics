/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.actors.extensions.metrics.dropwizard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;

import cloud.orbit.actors.extensions.NamedPipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.MessageDefinitions;
import cloud.orbit.concurrent.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension to collectdion Orbit messaging related metrics
 *
 * Created by jgong on 12/19/16.
 */
public class OrbitMessagingMetricsExtension extends NamedPipelineExtension
{
    public static final String MESSAGING_METRICS_PIPELINE_NAME = "messaging-metrics-pipeline";

    private Logger logger = LoggerFactory.getLogger(OrbitMessagingMetricsExtension.class);

    //Metrics Meters
    private Map<Integer, Meter> receiveMeters = new HashMap();
    private Map<Integer, Meter> sendMeters = new HashMap();


    public OrbitMessagingMetricsExtension()
    {
        super(MESSAGING_METRICS_PIPELINE_NAME, null, DefaultHandlers.MESSAGING);
        setupMetrics();
    }

    public OrbitMessagingMetricsExtension(final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        super(name, beforeHandlerName, afterHandlerName);
        setupMetrics();
    }


    private void setupMetrics()
    {
        //inbound
        receiveMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:one_way_message direction:inbound]"));
        receiveMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:request_message direction:inbound]"));
        receiveMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:response_error direction:inbound]"));
        receiveMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:response_ok direction:inbound]"));
        receiveMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:response_protocol_error direction:inbound]"));

        //outbound
        sendMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:one_way_message direction:outbound]"));
        sendMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:request_message direction:outbound]"));
        sendMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:response_error direction:outbound]"));
        sendMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:response_ok direction:outbound]"));
        sendMeters.put((int) MessageDefinitions.ONE_WAY_MESSAGE, MetricsManager.getInstance().getRegistry().meter("orbit.messaging[type:response_protocol_error direction:outbound]"));
    }

    @Override
    public void onRead(HandlerContext ctx, Object message)
    {
        if (message instanceof Message)
        {
            Meter meter = receiveMeters.get(((Message) message).getMessageType());
            if (null != meter)
            {
                meter.mark();
            }
        }
        ctx.fireRead(message);
    }

    @Override
    public Task write(HandlerContext ctx, Object message) throws Exception
    {
        if (message instanceof Message)
        {
            Meter meter = sendMeters.get(((Message) message).getMessageType());
            if (null != meter)
            {
                meter.mark();
            }
        }
        return ctx.write(message);
    }
}
