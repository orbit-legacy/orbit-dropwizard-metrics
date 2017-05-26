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
import com.codahale.metrics.Timer;

import cloud.orbit.actors.Actor;
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
    private static final Logger logger = LoggerFactory.getLogger(OrbitActorExtension.class);

    public static final String ACTOR_METRICS_PIPELINE_NAME = "actor-metrics-pipeline";

    //Timer, per actor instance
    Map<String, Timer.Context> actorActivationTimers = new ConcurrentHashMap<>();
    Map<String, Timer.Context> actorLifeSpanTimers = new ConcurrentHashMap<>();
    Map<String, Timer.Context> actorDeactivationTimers = new ConcurrentHashMap<>();

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
            MetricsManager.getInstance().getRegistry().meter(getActorTypeMessageReceiveRateMetricsKey(toClass)).mark();
        }
        ctx.fireRead(message);
    }

    @Override
    public Task<?> preActivation(final AbstractActor<?> actor)
    {
        String actorTypeActivationTimerKey = getActorTypeActivationMetricsKey(RemoteReference.getInterfaceClass(actor));
        Timer.Context timer = MetricsManager.getInstance().getRegistry().timer(actorTypeActivationTimerKey).time();
        String activationTimerKey = getActorTimerKey(actor);
        actorActivationTimers.put(activationTimerKey, timer);

        return Task.done();
    }

    @Override
    public Task<?> postActivation(final AbstractActor<?> actor)
    {
        //Actor count(per type)
        String actorCounterKey = getActorTypeCounterMetricsKey(RemoteReference.getInterfaceClass(actor));
        MetricsManager.getInstance().getRegistry().counter(actorCounterKey).inc();

        //activation timer
        Timer.Context actorActivationTimer = actorActivationTimers.remove(getActorTimerKey(actor));
        if (null != actorActivationTimer)
        {
            actorActivationTimer.stop();
        }

        //Actor lifespan
        String actorLifespanTimerKey = getActorTimerKey(actor);
        String actorTypeLifespanMetricsKey = getActorTypeLifeSpanMetricsKey(RemoteReference.getInterfaceClass(actor));

        Timer.Context timer = MetricsManager.getInstance().getRegistry().timer(actorTypeLifespanMetricsKey).time();
        actorLifeSpanTimers.put(actorLifespanTimerKey, timer);

        return Task.done();
    }

    @Override
    public Task<?> preDeactivation(final AbstractActor<?> actor)
    {
        //deactivation timer
        String actorDeactivationTimerKey = getActorTimerKey(actor);
        String actorDeactivationMetricsKey = getActorTypeDeactivationMetricsKey(RemoteReference.getInterfaceClass(actor));
        Timer.Context timer = MetricsManager.getInstance().getRegistry().timer(actorDeactivationMetricsKey).time();
        actorDeactivationTimers.put(actorDeactivationTimerKey, timer);

        //Actor lifespan
        String actorLifespanTimerKey = getActorTimerKey(actor);
        String actorTypeLifespanMetricsKey = getActorTypeLifeSpanMetricsKey(RemoteReference.getInterfaceClass(actor));
        Timer.Context lifespanTimer = actorLifeSpanTimers.remove(actorLifespanTimerKey);
        if (null != lifespanTimer)
        {
            lifespanTimer.stop();
        }

        return Task.done();
    }

    @Override
    public Task<?> postDeactivation(final AbstractActor<?> actor)
    {
        //actor counter per type and node
        String actorCounterKey = getActorTypeCounterMetricsKey(RemoteReference.getInterfaceClass(actor));
        MetricsManager.getInstance().getRegistry().counter(actorCounterKey).dec();

        //actor deactivation
        String actorDeactivationTimerKey = getActorTimerKey(actor);
        Timer.Context deactivationTimer = actorDeactivationTimers.remove(actorDeactivationTimerKey);
        if (null != deactivationTimer)
        {
            deactivationTimer.stop();
        }

        return Task.done();
    }

    public static String getActorTimerKey(final AbstractActor<?> actor)
    {
        Object id = RemoteReference.getId(actor);
        return String.format("actors.%s.%s.timer", RemoteReference.getInterfaceClass(actor).getSimpleName(), id);
    }

    /**
     * Metrics key: Actor message receive rate
     *
     * @param actorClass
     * @return
     */
    public static String getActorTypeMessageReceiveRateMetricsKey(Class<? extends Actor> actorClass)
    {
        return String.format("orbit.actors.msg_received_rate[[actor:%s]]", actorClass.getSimpleName());
    }

    /**
     * Metrics key: Actor count for a type on one node
     *
     * @param actorClass
     * @return
     */
    public static String getActorTypeCounterMetricsKey(Class<? extends Actor> actorClass)
    {
        return String.format("orbit.actors.count[actor:%s]", actorClass.getSimpleName());
    }

    /**
     * Metrics key: lifespan timer for a type of actor
     *
     * @param actorClass
     * @return
     */
    public static String getActorTypeLifeSpanMetricsKey(Class<? extends Actor> actorClass)
    {
        return String.format("orbit.actors.lifespan[actor:%s]", actorClass.getSimpleName());
    }

    /**
     * Metrics key: Actor activation time, per actor type
     *
     * @param actorClass
     * @return
     */
    public static String getActorTypeActivationMetricsKey(Class<? extends Actor> actorClass)
    {
        return String.format("orbit.actors.activation_time[actor:%s]", actorClass.getSimpleName());
    }

    /**
     * Metrics key: Actor deactivation time, per actor type
     *
     * @param actorClass
     * @return
     */
    public static String getActorTypeDeactivationMetricsKey(Class<? extends Actor> actorClass)
    {
        return String.format("orbit.actors.deactivation_time[actor:%s]", actorClass.getSimpleName());
    }
}