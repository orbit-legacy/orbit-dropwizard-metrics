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

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import cloud.orbit.actors.Stage;

import java.util.*;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrbitExecutionPoolMetricsTest
{
    private Stage stage;
    private OrbitExecutionPoolMetrics orbitExecutionPoolMetrics;

    @Before
    public void setUp() throws Exception
    {
        stage = new Stage();
        orbitExecutionPoolMetrics = new OrbitExecutionPoolMetrics(stage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stageNotStarted_throws() throws Exception
    {
        orbitExecutionPoolMetrics.register();
    }

    @Test
    public void stageHasExecutionPoolSet_ok() throws Exception
    {
        stage.setExecutionPool(Executors.newCachedThreadPool());
        orbitExecutionPoolMetrics.register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void stageHasUnsupportedThreadPoolType_throws() throws Exception
    {
        stage.setExecutionPool(Executors.unconfigurableExecutorService(Executors.newCachedThreadPool()));
        orbitExecutionPoolMetrics.register();
    }

    @Test
    public void metricsNotRegistered_noMetrics() throws Exception
    {
        stage.setExecutionPool(Executors.newWorkStealingPool());
        assertEquals(orbitExecutionPoolMetrics.getMetricRegistry().getMetrics(), Collections.EMPTY_MAP);
    }

    @Test
    public void constructedWithExistingMetricRegistry_usesExistingMetricRegistry() throws Exception
    {
        MetricRegistry metricRegistry = new MetricRegistry();
        assertEquals(metricRegistry, new OrbitExecutionPoolMetrics(stage, metricRegistry).getMetricRegistry());
    }

    @Test
    public void forkJoinPoolRegistered_hasForkJoinPoolMetrics() throws Exception
    {
        stage.setExecutionPool(Executors.newWorkStealingPool());
        orbitExecutionPoolMetrics.register();
        Set<String> expected = new HashSet<>();
        expected.add("orbit.stage.executionPool.activeThreadCount");
        expected.add("orbit.stage.executionPool.poolSize");
        expected.add("orbit.stage.executionPool.queuedSubmissionCount");
        expected.add("orbit.stage.executionPool.runningThreadCount");
        expected.add("orbit.stage.executionPool.queuedTaskCount");
        expected.add("orbit.stage.executionPool.stealCount");
        assertEquals(expected, orbitExecutionPoolMetrics.getMetricRegistry().getMetrics().keySet());
    }

    @Test
    public void threadPoolExecutorRegistered_hasThreadPoolExecutorMetrics() throws Exception
    {
        stage.setExecutionPool(Executors.newCachedThreadPool());
        orbitExecutionPoolMetrics.register();
        Set<String> expected = new HashSet<>();
        expected.add("orbit.stage.executionPool.activeThreadCount");
        expected.add("orbit.stage.executionPool.poolSize");
        expected.add("orbit.stage.executionPool.completedTaskCount");
        expected.add("orbit.stage.executionPool.largestPoolSize");
        expected.add("orbit.stage.executionPool.taskCount");
        expected.add("orbit.stage.executionPool.remainingCapacity");
        assertEquals(expected, orbitExecutionPoolMetrics.getMetricRegistry().getMetrics().keySet());
    }

    @Test
    public void forkJoinPoolIsActive_metricsAreAccurate() throws Exception
    {
        stage.setExecutionPool(Executors.newWorkStealingPool());
        orbitExecutionPoolMetrics.register();
        Runnable emptyTask = () ->
        {
        };
        stage.getExecutionPool().submit(emptyTask).get();
        final Map<String, Gauge> metrics = orbitExecutionPoolMetrics.getMetricRegistry().getGauges();
        assertEquals(0, metrics.get("orbit.stage.executionPool.activeThreadCount").getValue());
        assertTrue((int) metrics.get("orbit.stage.executionPool.poolSize").getValue() > 0);
    }

    @Test
    public void threadPoolExecutorIsActive_metricsAreAccurate() throws Exception
    {
        stage.setExecutionPool(Executors.newCachedThreadPool());
        orbitExecutionPoolMetrics.register();
        Runnable emptyTask = () ->
        {
        };
        long taskCount = 10;
        for (int i = 0; i < taskCount; i++)
        {
            stage.getExecutionPool().submit(emptyTask).get();
        }
        final Map<String, Gauge> metrics = orbitExecutionPoolMetrics.getMetricRegistry().getGauges();
        assertEquals(0, metrics.get("orbit.stage.executionPool.activeThreadCount").getValue());
        assertEquals(taskCount, metrics.get("orbit.stage.executionPool.completedTaskCount").getValue());
        assertTrue((int) metrics.get("orbit.stage.executionPool.poolSize").getValue() > 0);
    }
}
