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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.extensions.NamedPipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pipeline Extension to collect the actor's metrics: number of actor, actor lifespans distribution, actor message received rate
 *
 * Created by jgong on 12/16/16.
 */
public class OrbitActorExtension extends NamedPipelineExtension implements LifetimeExtension
{
    public static final String ACTOR_METRICS_PIPELINE_NAME = "actor-metrics-pipeline";

    private Logger logger = LoggerFactory.getLogger(OrbitActorExtension.class);

    //Actor Counter, per actor type and node, actor count breakdown to each type
    Map<String, Counter> actorCounters = new HashMap<>();

    //Timer, per actor instance
    Map<String, Long> actorStartTimes = new ConcurrentHashMap<>();

    Map<String, Histogram> actorLifeSpanHistograms = new HashMap<>();

    Map<String, Meter> actorMsgReceivedRate = new HashMap<>();


    public OrbitActorExtension()
    {
        super(ACTOR_METRICS_PIPELINE_NAME, null, DefaultHandlers.EXECUTION);
    }

    public OrbitActorExtension(final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        super(name, beforeHandlerName, afterHandlerName);
    }

    @Override
    public void onRead(HandlerContext ctx, Object message)
    {
        if (message instanceof Invocation)
        {
            Invocation invocation = (Invocation) message;
            //process invocation metrics
            final RemoteReference toReference = invocation.getToReference();
            Class toClass = RemoteReference.getInterfaceClass(toReference);
            Object toId = RemoteReference.getId(toReference);
            Meter msgReceivedMeter = actorMsgReceivedRate.get(toClass.getSimpleName());
            if (null == msgReceivedMeter)
            {
                msgReceivedMeter = MetricsManager.getInstance().getRegistry().meter(getActorMessageReceiveRateMetricKey(toClass));
                actorMsgReceivedRate.put(toClass.getSimpleName(), msgReceivedMeter);
            }
            msgReceivedMeter.mark();

        }
        ctx.fireRead(message);
    }

    @Override
    public Task write(HandlerContext ctx, Object message) throws Exception
    {
        if (message instanceof Invocation)
        {
            Class toClass = RemoteReference.getInterfaceClass(((Invocation) message).getToReference());
        }
        return ctx.write(message);
    }

    @Override
    public Task<?> postActivation(final AbstractActor<?> actor)
    {
        Object id = RemoteReference.getId(actor);
        //Actor count(per type)
        String actorCounterKey = getActorCounterMetricsKey(RemoteReference.getInterfaceClass(actor));
        Counter counter = MetricsManager.getInstance().getRegistry().counter(actorCounterKey);
        if (null == counter)
        {
            counter = MetricsManager.getInstance().getRegistry().counter(actorCounterKey);
            actorCounters.put(actorCounterKey, counter);
        }
        counter.inc();

        //Actor lifespan
        String startTimeKey = getActorStartTimeKey(actor);

        actorStartTimes.put(startTimeKey, System.currentTimeMillis());

        String actorLifeSpanHistogramKey = getActorLifeSpanHistogramMetricsKey(RemoteReference.getInterfaceClass(actor));
        Histogram histogram = actorLifeSpanHistograms.get(actorLifeSpanHistogramKey);
        if (null == histogram)
        {
            histogram = MetricsManager.getInstance().getRegistry().histogram(getActorLifeSpanHistogramMetricsKey(RemoteReference.getInterfaceClass(actor)));
            actorLifeSpanHistograms.put(actorLifeSpanHistogramKey, histogram);
        }


        return Task.done();
    }

    @Override
    public Task<?> postDeactivation(final AbstractActor<?> actor)
    {
        //actor counter per type and node
        String actorCounterKey = getActorCounterMetricsKey(RemoteReference.getInterfaceClass(actor));
        Counter counter = MetricsManager.getInstance().getRegistry().counter(actorCounterKey);
        counter.dec();

        //actor lifespan
        String startTimeKey = getActorStartTimeKey(actor);

        Long starTime = actorStartTimes.get(startTimeKey);
        if (null != starTime)
        {
            if (null != starTime)
            {
                actorStartTimes.remove(startTimeKey);
            }

            String actorLifeSpanHistogramKey = getActorLifeSpanHistogramMetricsKey(RemoteReference.getInterfaceClass(actor));
            Histogram histogram = actorLifeSpanHistograms.get(actorLifeSpanHistogramKey);
            if (null != histogram)
            {
                histogram.update(System.currentTimeMillis() - starTime);
            }
        }
        return Task.done();
    }

    public static String getActorStartTimeKey(final AbstractActor<?> actor)
    {
        Object id = RemoteReference.getId(actor);
        return String.format("actors.%s.%s.starttime", RemoteReference.getInterfaceClass(actor).getSimpleName(), id);
    }

    /**
     * Metric key: Actor message receive rate
     *
     * @param actorClass
     * @return
     */
    public static String getActorMessageReceiveRateMetricKey(Class actorClass)
    {
        return String.format("orbit.actors.msg_received_rate[[actor:%s]]", actorClass.getSimpleName());
    }

    /**
     * Metrics key: Actor count for a type on one node
     *
     * @param actorClass
     * @return
     */
    public static String getActorCounterMetricsKey(Class actorClass)
    {
        return String.format("orbit.actors.count[actor:%s]", actorClass.getSimpleName());
    }

    /**
     * Metric key: lifespan histogram for a type of actor
     *
     * @param actorClass
     * @return
     */
    public static String getActorLifeSpanHistogramMetricsKey(Class actorClass)
    {
        return String.format("orbit.actors.lifespanhistogram[actor:%s]", actorClass.getSimpleName());
    }
}
