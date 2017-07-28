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

import com.codahale.metrics.MetricRegistry;
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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pipeline Extension to collect the actor's metrics: number of actor, actor lifetimes distribution, actor message received rate
 *
 * Created by jgong on 12/16/16.
 */
public class OrbitMetricsActorExtension extends NamedPipelineExtension implements LifetimeExtension
{
    private static final Logger logger = LoggerFactory.getLogger(OrbitMetricsActorExtension.class);

    public static final String ACTOR_METRICS_PIPELINE_NAME = "actor-metrics-pipeline";

    private Map<String, Timer.Context> actorActivationTimers = new ConcurrentHashMap<>();
    private Map<String, Timer.Context> actorLifetimeTimers = new ConcurrentHashMap<>();
    private Map<String, Timer.Context> actorDeactivationTimers = new ConcurrentHashMap<>();

    private MetricRegistry metricRegistry;

    public OrbitMetricsActorExtension()
    {
        this(new MetricRegistry());
    }

    public OrbitMetricsActorExtension(MetricRegistry metricRegistry)
    {
        this(metricRegistry, ACTOR_METRICS_PIPELINE_NAME, null, DefaultHandlers.EXECUTION);
    }
    
    public OrbitMetricsActorExtension(final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        this(new MetricRegistry(), name, beforeHandlerName, afterHandlerName);
    }
    
    public OrbitMetricsActorExtension(final MetricRegistry metricRegistry, final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        super(name, beforeHandlerName, afterHandlerName);
        this.metricRegistry = metricRegistry;
    }
    
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
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
            Method method = invocation.getMethod();
            metricRegistry.meter(getActorTypeMessageReceiveRateMetricsKey(toClass, method)).mark();
        }
        ctx.fireRead(message);
    }

    @Override
    public Task<?> preActivation(final AbstractActor<?> actor)
    {
        String actorTypeActivationMetricsKey = getActorTypeActivationMetricsKey(RemoteReference.getInterfaceClass(actor));
        Timer.Context timer = metricRegistry.timer(actorTypeActivationMetricsKey).time();
        String actorActivationTimerKey = getActorTimerKey(actor);
        actorActivationTimers.put(actorActivationTimerKey, timer);

        return Task.done();
    }

    @Override
    public Task<?> postActivation(final AbstractActor<?> actor)
    {
        Timer.Context actorActivationTimer = actorActivationTimers.remove(getActorTimerKey(actor));
        if (null != actorActivationTimer)
        {
            actorActivationTimer.stop();
        }
        
        String actorCounterKey = getActorTypeCounterMetricsKey(RemoteReference.getInterfaceClass(actor));
        metricRegistry.counter(actorCounterKey).inc();

        String actorTypeLifetimeMetricsKey = getActorTypeLifetimeMetricsKey(RemoteReference.getInterfaceClass(actor));
        Timer.Context timer = metricRegistry.timer(actorTypeLifetimeMetricsKey).time();
        String actorLifetimeTimerKey = getActorTimerKey(actor);
        actorLifetimeTimers.put(actorLifetimeTimerKey, timer);

        return Task.done();
    }

    @Override
    public Task<?> preDeactivation(final AbstractActor<?> actor)
    {
        Timer.Context lifetimeTimer = actorLifetimeTimers.remove(getActorTimerKey(actor));
        if (null != lifetimeTimer)
        {
            lifetimeTimer.stop();
        }

        String actorCounterKey = getActorTypeCounterMetricsKey(RemoteReference.getInterfaceClass(actor));
        metricRegistry.counter(actorCounterKey).dec();

        String actorDeactivationMetricsKey = getActorTypeDeactivationMetricsKey(RemoteReference.getInterfaceClass(actor));
        Timer.Context timer = metricRegistry.timer(actorDeactivationMetricsKey).time();
        String actorDeactivationTimerKey = getActorTimerKey(actor);
        actorDeactivationTimers.put(actorDeactivationTimerKey, timer);

        return Task.done();
    }

    @Override
    public Task<?> postDeactivation(final AbstractActor<?> actor)
    {
        Timer.Context deactivationTimer = actorDeactivationTimers.remove(getActorTimerKey(actor));
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
    public static String getActorTypeMessageReceiveRateMetricsKey(Class<? extends Actor> actorClass, Method actorMethod)
    {
        return String.format("orbit.actors.msg_received_rate[actor:%s,method:%s]", actorClass.getSimpleName(), actorMethod.getName());
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
     * Metrics key: lifetime timer for a type of actor
     *
     * @param actorClass
     * @return
     */
    public static String getActorTypeLifetimeMetricsKey(Class<? extends Actor> actorClass)
    {
        return String.format("orbit.actors.lifetime[actor:%s]", actorClass.getSimpleName());
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