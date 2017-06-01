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
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import cloud.orbit.actors.extensions.NamedPipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.MessageDefinitions;
import cloud.orbit.concurrent.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Extension to collect Orbit messaging related metrics
 *
 * Created by jgong on 12/19/16.
 */
public class OrbitMetricsMessagingExtension extends NamedPipelineExtension
{
    private static final Logger logger = LoggerFactory.getLogger(OrbitMetricsMessagingExtension.class);
  
    private static final String MESSAGING_METRICS_PIPELINE_NAME = "messaging-metrics-pipeline";
    private static final String MESSAGING_METRICS_UNTIMED = "orbit.messaging.untimed";
    private static final String MESSAGING_METRICS_HEADER_TIMESTAMP = "metrics-ts";


    private Map<Integer, Timer> inboundMetrics = new HashMap<>();
    private Map<Integer, Meter> outboundMetrics = new HashMap<>();

    private MetricRegistry metricRegistry;

    public OrbitMetricsMessagingExtension()
    {
        this(new MetricRegistry());
    }
    
    public OrbitMetricsMessagingExtension(MetricRegistry metricRegistry) {
        this(metricRegistry, MESSAGING_METRICS_PIPELINE_NAME, null, DefaultHandlers.MESSAGING);
    }

    public OrbitMetricsMessagingExtension(final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        this(new MetricRegistry(), name, beforeHandlerName, afterHandlerName);
    }
    
    public OrbitMetricsMessagingExtension(MetricRegistry metricRegistry, final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        super(name, beforeHandlerName, afterHandlerName);
        this.metricRegistry = metricRegistry;
        setupMetrics();
    }

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    private void setupMetrics()
    {
        inboundMetrics.put((int) MessageDefinitions.ONE_WAY_MESSAGE, metricRegistry.timer("orbit.messaging[type:one_way_message,direction:inbound]"));
        inboundMetrics.put((int) MessageDefinitions.REQUEST_MESSAGE, metricRegistry.timer("orbit.messaging[type:request_message,direction:inbound]"));
        inboundMetrics.put((int) MessageDefinitions.RESPONSE_ERROR, metricRegistry.timer("orbit.messaging[type:response_error,direction:inbound]"));
        inboundMetrics.put((int) MessageDefinitions.RESPONSE_OK, metricRegistry.timer("orbit.messaging[type:response_ok,direction:inbound]"));
        inboundMetrics.put((int) MessageDefinitions.RESPONSE_PROTOCOL_ERROR, metricRegistry.timer("orbit.messaging[type:response_protocol_error,direction:inbound]"));

        outboundMetrics.put((int) MessageDefinitions.ONE_WAY_MESSAGE, metricRegistry.meter("orbit.messaging[type:one_way_message,direction:outbound]"));
        outboundMetrics.put((int) MessageDefinitions.REQUEST_MESSAGE, metricRegistry.meter("orbit.messaging[type:request_message,direction:outbound]"));
        outboundMetrics.put((int) MessageDefinitions.RESPONSE_ERROR, metricRegistry.meter("orbit.messaging[type:response_error,direction:outbound]"));
        outboundMetrics.put((int) MessageDefinitions.RESPONSE_OK, metricRegistry.meter("orbit.messaging[type:response_ok,direction:outbound]"));
        outboundMetrics.put((int) MessageDefinitions.RESPONSE_PROTOCOL_ERROR, metricRegistry.meter("orbit.messaging[type:response_protocol_error,direction:outbound]"));
    }

    @Override
    public void onRead(HandlerContext ctx, Object object)
    {
        long now = System.currentTimeMillis();
        if (object instanceof Message)
        {
            Message message = (Message) object;
            Long messageCreationTimestamp = (Long) message.getHeader(MESSAGING_METRICS_HEADER_TIMESTAMP);
            if(messageCreationTimestamp != null)
            {
                Timer metric = inboundMetrics.get(message.getMessageType());
                if (metric != null)
                {
                    metric.update(now - messageCreationTimestamp, TimeUnit.MILLISECONDS);
                }
            }
            else
            {
                metricRegistry.meter(MESSAGING_METRICS_UNTIMED + "[direction:inbound]").mark();
            }
        }
        ctx.fireRead(object);
    }

    @Override
    public Task<?> write(HandlerContext ctx, Object object) throws Exception
    {
        if (object instanceof Message)
        {
            Message message = (Message) object;
            Meter metric = outboundMetrics.get(message.getMessageType());
            if (metric != null)
            {
                metric.mark();
            }
            
            message.setHeader(MESSAGING_METRICS_HEADER_TIMESTAMP, Long.valueOf(System.currentTimeMillis()));
        }
        return ctx.write(object);
    }
}
