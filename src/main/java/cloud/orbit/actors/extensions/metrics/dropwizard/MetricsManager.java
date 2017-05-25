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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;

import java.util.List;

/**
 * Created by jgong on 11/29/16.
 */
public class MetricsManager {
    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);
    
    private static MetricsManager instance = new MetricsManager();
    
    private MetricRegistry registry;
    private boolean isInitialized = false;

    public static MetricsManager getInstance() {
        return instance;
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

    public synchronized void initializeMetrics(List<ReporterConfig> reporterConfigs) {
        if (isInitialized) {
          logger.warn("Attempting to initialize the Metrics Manager when it is already initialized!");
          return;
        }
        
        for (ReporterConfig reporterConfig : reporterConfigs) {
            Reporter reporter = reporterConfig.enableReporter(registry);
            if (reporter == null) {
                logger.warn("Failed to enable reporter " + reporterConfig.getClass().getName());
            }
        }
        isInitialized = true;
        logger.info("Orbit Metrics Initialized.");
    }

    public void registerMetric(String name, Metric metric) {
        try {
            registry.register(name, metric);
            logger.debug("Registered new metric {}", name);
        } catch (IllegalArgumentException iae) {
            logger.warn("Unable to register metric {} because a metric already has been registered with the same name", name);
        }
    }

    public void unregisterMetric(String name) {
        registry.remove(name);
    }

    public void setRegistry(MetricRegistry registry) {
        this.registry = registry;
        this.isInitialized = false;
    }
}
