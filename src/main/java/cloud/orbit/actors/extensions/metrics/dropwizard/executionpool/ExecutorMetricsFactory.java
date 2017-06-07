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

import com.codahale.metrics.*;

import java.util.*;
import java.util.concurrent.*;


public class ExecutorMetricsFactory
{
    /**
     * Builds a set of metrics from an {@link Executor} which may vary depending on its implementation.
     */
    public Metric create(final Executor executor)
    {
        if (executor instanceof ForkJoinPool)
        {
            return new ForkJoinPoolMetrics((ForkJoinPool) executor);
        }
        if (executor instanceof ThreadPoolExecutor)
        {
            return new ThreadPoolExecutorMetrics((ThreadPoolExecutor) executor);
        }
        throw new IllegalArgumentException(String.format("Executor implementation %s is not supported",
                executor.getClass().getSimpleName()));
    }

    public static class ForkJoinPoolMetrics implements MetricSet
    {
        private final ForkJoinPool pool;

        public ForkJoinPoolMetrics(final ForkJoinPool pool)
        {
            this.pool = pool;
        }

        @Override
        public Map<String, Metric> getMetrics()
        {
            Map<String, Metric> metrics = new HashMap<>();
            metrics.put("activeThreadCount", (Gauge<Integer>) pool::getActiveThreadCount);
            metrics.put("poolSize", (Gauge<Integer>) pool::getPoolSize);
            metrics.put("queuedSubmissionCount", (Gauge<Integer>) pool::getQueuedSubmissionCount);
            metrics.put("runningThreadCount", (Gauge<Integer>) pool::getRunningThreadCount);
            metrics.put("queuedTaskCount", (Gauge<Long>) pool::getQueuedTaskCount);
            metrics.put("stealCount", (Gauge<Long>) pool::getStealCount);
            return Collections.unmodifiableMap(metrics);
        }
    }

    public static class ThreadPoolExecutorMetrics implements MetricSet
    {
        private final ThreadPoolExecutor pool;

        public ThreadPoolExecutorMetrics(final ThreadPoolExecutor pool)
        {
            this.pool = pool;
        }

        @Override
        public Map<String, Metric> getMetrics()
        {
            Map<String, Metric> metrics = new HashMap<>();
            metrics.put("activeThreadCount", (Gauge<Integer>) pool::getActiveCount);
            metrics.put("poolSize", (Gauge<Integer>) pool::getPoolSize);
            metrics.put("completedTaskCount", (Gauge<Long>) pool::getCompletedTaskCount);
            metrics.put("largestPoolSize", (Gauge<Integer>) pool::getLargestPoolSize);
            metrics.put("taskCount", (Gauge<Long>) pool::getTaskCount);
            metrics.put("remainingCapacity", (Gauge<Integer>) () -> pool.getQueue().remainingCapacity());
            return Collections.unmodifiableMap(metrics);
        }
    }
}
