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

import com.codahale.metrics.MetricRegistry;

import cloud.orbit.actors.runtime.DefaultInvocationHandler;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.RemoteReference;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created by jgong on 12/19/16.
 */
public class OrbitMetricsInvocationHandler extends DefaultInvocationHandler
{
    private MetricRegistry metricRegistry;

    public OrbitMetricsInvocationHandler() {
        this(new MetricRegistry());
    }
    
    public OrbitMetricsInvocationHandler(MetricRegistry metricRegistry) {
        super();
        this.metricRegistry = metricRegistry;
    }
    
    @Override
    public void afterInvoke(final long startTimeNanos, final Invocation invocation, final Method method)
    {
        final RemoteReference<?> toReference = invocation.getToReference();
        super.afterInvoke(startTimeNanos, invocation, method);
        final long durationNanos = (System.nanoTime() - startTimeNanos);
        Class<?> actorClass = RemoteReference.getInterfaceClass(toReference);
        String metricsKey = getActorMethodResponseTimeMetricsKey(actorClass, method.getName());

        metricRegistry.timer(metricsKey).update(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void taskComplete(long startTimeNanos, Invocation invocation, Method method)
    {
        final RemoteReference<?> toReference = invocation.getToReference();
        super.afterInvoke(startTimeNanos, invocation, method);
        final long durationNanos = (System.nanoTime() - startTimeNanos);
        Class<?> actorClass = RemoteReference.getInterfaceClass(toReference);
        String metricsKey = getActorChainResponseTimeMetricsKey(actorClass, method.getName());
        metricRegistry.timer(metricsKey).update(durationNanos, TimeUnit.NANOSECONDS);
    }


    public static String getActorMethodResponseTimeMetricsKey(Class<?> actorClass, String methodName)
    {
        return String.format("orbit.actors.methodresponsetime[actor:%s,method:%s]", actorClass.getSimpleName(), methodName);
    }

    public static String getActorChainResponseTimeMetricsKey(Class<?> actorClass, String methodName)
    {
        return String.format("orbit.actors.chainresponsetime[actor:%s,method:%s]", actorClass.getSimpleName(), methodName);
    }
}
