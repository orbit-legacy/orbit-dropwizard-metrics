/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.extensions.metrics.dropwizard.executionpool;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import cloud.orbit.actors.Stage;

import java.util.concurrent.ExecutorService;

public class OrbitExecutionPoolMetrics
{
    private final Stage stage;
    private final MetricRegistry metricRegistry;
    private final ExecutorMetricsFactory executorMetricsFactory;

    public OrbitExecutionPoolMetrics(final Stage stage,
                                     final MetricRegistry metricRegistry,
                                     final ExecutorMetricsFactory executorMetricsFactory)
    {
        this.stage = stage;
        this.metricRegistry = metricRegistry;
        this.executorMetricsFactory = executorMetricsFactory;
    }

    public OrbitExecutionPoolMetrics(final Stage stage, final MetricRegistry metricRegistry)
    {
        this(stage, metricRegistry, new ExecutorMetricsFactory());
    }

    public OrbitExecutionPoolMetrics(final Stage stage)
    {
        this(stage, new MetricRegistry());
    }

    public MetricRegistry getMetricRegistry()
    {
        return metricRegistry;
    }

    /**
     * Register metrics from the {@link Stage}'s execution pool with the {@link MetricRegistry}.
     *
     * Unless the stage was constructed with an execution pool, it is necessary
     * to start the stage before calling this method.
     *
     * @see Stage#start()
     */
    public void register()
    {
        if (stage == null)
        {
            throw new IllegalArgumentException("null stage");
        }
        ExecutorService pool = stage.getExecutionPool();
        if (pool == null)
        {
            throw new IllegalArgumentException("null execution pool. Has the stage been started?");
        }
        Metric poolMetrics = executorMetricsFactory.create(pool);
        metricRegistry.register("orbit.stage.executionPool", poolMetrics);
    }
}
