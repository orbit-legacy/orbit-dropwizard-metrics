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

import com.codahale.metrics.Histogram;

import cloud.orbit.actors.runtime.DefaultInvocationHandler;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.RemoteReference;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jgong on 12/19/16.
 */
public class OrbitMetricsInvocationHandler extends DefaultInvocationHandler
{
    private Map<String, Histogram> actorMethodResponseTimeHistograms = new ConcurrentHashMap<>();
    private Map<String, Histogram> actorChainResponseTimeHistograms = new ConcurrentHashMap<>();

    @Override
    public void afterInvoke(final long startTimeNanos, final Invocation invocation, final Method method) {
        final RemoteReference toReference = invocation.getToReference();
        super.afterInvoke(startTimeNanos, invocation, method);
        final long durationNanos = (System.nanoTime() - startTimeNanos);
        final Double durationMs = durationNanos / 1_000_000.0;
        Class actorClass = RemoteReference.getInterfaceClass(toReference);
        String histKey = getActorMethodResponseTimeMetricKey(actorClass, method.getName());
        Histogram hist = actorMethodResponseTimeHistograms.get(histKey);
        if (null == hist) {
            hist = MetricsManager.getInstance().getRegistry().histogram(histKey);
            actorMethodResponseTimeHistograms.put(histKey, hist);
        }
        hist.update(durationMs.intValue());
    }

    @Override
    public void taskComplete(long startTimeNanos, Invocation invocation, Method method) {
        final RemoteReference toReference = invocation.getToReference();
        super.afterInvoke(startTimeNanos, invocation, method);
        final long durationNanos = (System.nanoTime() - startTimeNanos);
        final Double durationMs = durationNanos / 1_000_000.0;
        Class actorClass = RemoteReference.getInterfaceClass(toReference);
        String histKey = getActorChainResponseTimeMetricKey(actorClass, method.getName());
        Histogram hist = actorChainResponseTimeHistograms.get(histKey);
        if (null == hist) {
            hist = MetricsManager.getInstance().getRegistry().histogram(histKey);
            actorChainResponseTimeHistograms.put(histKey, hist);
        }
        hist.update(durationMs.intValue());
    }


    public static String getActorMethodResponseTimeMetricKey(Class actorClass, String methodName) {
        return String.format("orbit.actors.methodresponsetimehistogram[actor:%s,method:%s]", actorClass.getSimpleName(), methodName);
    }

    public static String getActorChainResponseTimeMetricKey(Class actorClass, String methodName) {
        return String.format("orbit.actors.chainresponsetimehistogram[actor:%s,method:%s]", actorClass.getSimpleName(), methodName);
    }
}
