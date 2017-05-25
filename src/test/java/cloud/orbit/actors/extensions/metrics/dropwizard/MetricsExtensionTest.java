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

package cloud.orbit.actors.extensions.metrics.dropwizard;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.codahale.metrics.MetricRegistry;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by asnyder on 2/2/17.
 */
public class MetricsExtensionTest
{
    @Mock
    private ReporterConfig reporterConfig;

    @Before
    public void setUp() throws Exception
    {
        initMocks(this);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructWithRegistry_nullArgument_throws() throws Exception
    {
        new MetricsExtension((MetricRegistry) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructWithConfigs_nullArgument_throws() throws Exception
    {
        new MetricsExtension((List<ReporterConfig>) null);
    }

    @Test
    public void constructEmpty_hasRegistry() throws Exception
    {
        new MetricsExtension();
        assertNotNull(MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void constructWithRegistry_hasRegistry() throws Exception
    {
        MetricRegistry registry = new MetricRegistry();
        new MetricsExtension(registry);
        assertEquals(registry, MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void constructWithConfigs_hasRegistry() throws Exception
    {
        new MetricsExtension(Collections.emptyList());
        assertNotNull(MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void start_constructedEmpty_registersReporter() throws Exception
    {
        List<ReporterConfig> configs = Collections.singletonList(reporterConfig);
        MetricsExtension metricsExtension = new MetricsExtension();
        injectConfig(metricsExtension, configs);
        metricsExtension.start().join();
        verify(reporterConfig).enableReporter(MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void start_constructedWithConfigs_registersReporter() throws Exception
    {
        List<ReporterConfig> configs = Collections.singletonList(reporterConfig);
        MetricsExtension metricsExtension = new MetricsExtension(configs);
        metricsExtension.start().join();
        verify(reporterConfig).enableReporter(MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void start_constructedWithRegistry_registersReporter() throws Exception
    {
        List<ReporterConfig> configs = Collections.singletonList(reporterConfig);
        MetricRegistry registry = new MetricRegistry();
        MetricsExtension metricsExtension = new MetricsExtension(registry);
        injectConfig(metricsExtension, configs);
        metricsExtension.start().join();
        verify(reporterConfig).enableReporter(MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void start_constructedEmpty_hasSameRegistry() throws Exception
    {
        MetricsExtension metricsExtension = new MetricsExtension();
        final MetricRegistry registry = MetricsManager.getInstance().getRegistry();
        metricsExtension.start().join();
        assertEquals(registry, MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void start_constructedWithRegistry_hasSameRegistry() throws Exception
    {
        final MetricRegistry registry = new MetricRegistry();
        MetricsExtension metricsExtension = new MetricsExtension(registry);
        metricsExtension.start().join();
        assertEquals(registry, MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void start_constructedWithConfigs_hasSameRegistry() throws Exception
    {
        MetricsExtension metricsExtension = new MetricsExtension(Collections.emptyList());
        final MetricRegistry registry = MetricsManager.getInstance().getRegistry();
        metricsExtension.start().join();
        assertEquals(registry, MetricsManager.getInstance().getRegistry());
    }

    private static void injectConfig(final MetricsExtension metricsExtension, final List<ReporterConfig> metricsConfig)
            throws NoSuchFieldException, IllegalAccessException
    {
        Field f = MetricsExtension.class.getDeclaredField("metricsConfig");
        f.setAccessible(true);
        f.set(metricsExtension, metricsConfig);
    }
}
