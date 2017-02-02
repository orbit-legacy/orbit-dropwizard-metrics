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

import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by asnyder on 2/2/17.
 */
public class MetricsExtensionTest
{
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
        MetricsManager.getInstance().setRegistry(null);
        new MetricsExtension();
        assertNotNull(MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void constructWithRegistry_hasRegistry() throws Exception
    {
        MetricRegistry registry = new MetricRegistry();
        MetricsManager.getInstance().setRegistry(null);
        new MetricsExtension(registry);
        assertEquals(registry, MetricsManager.getInstance().getRegistry());
    }

    @Test
    public void constructWithConfigs_hasRegistry() throws Exception
    {
        MetricsManager.getInstance().setRegistry(null);
        new MetricsExtension(Collections.emptyList());
        assertNotNull(MetricsManager.getInstance().getRegistry());
    }
}