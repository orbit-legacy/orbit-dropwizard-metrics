Orbit Dropwizard Metrics Extension
============
[![Release](https://img.shields.io/github/release/orbit/orbit-dropwizard-metrics.svg)](https://github.com/orbit/orbit-dropwizard-metrics/releases)
[![Maven Central](https://img.shields.io/maven-central/v/cloud.orbit/orbit-dropwizard-metrics.svg)](https://repo1.maven.org/maven2/cloud/orbit/orbit-dropwizard-metrics/)
[![Javadocs](https://img.shields.io/maven-central/v/cloud.orbit/orbit-dropwizard-metrics.svg?label=javadocs)](http://www.javadoc.io/doc/cloud.orbit/orbit-dropwizard-metrics)
[![Build Status](https://img.shields.io/travis/orbit/orbit-dropwizard-metrics.svg)](https://travis-ci.org/orbit/orbit-dropwizard-metrics)
[![Gitter](https://img.shields.io/badge/style-Join_Chat-ff69b4.svg?style=flat&label=gitter)](https://gitter.im/orbit/orbit?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Developer & License
======
This project was developed by [Electronic Arts](http://www.ea.com) and is licensed under the [BSD 3-Clause License](LICENSE).

Introduction
======
The Metrics Extension is a wrapper of Dropwizard Metrics library - http://metrics.dropwizard.io/. Out of the box, it support four different reporters: Ganglia, Graphite, JMX and Datadog. 
It also contains Orbit extensions and handler for user to collect the metrics of Orbit cluster. 

Examples
======
To Use The Extension
-----
Modify Orbit configuration to include the extension: 
```
      !!cloud.orbit.actors.extensions.metrics.dropwizard.MetricsExtension {
        metricsConfig:
        [
          !!cloud.orbit.actors.extensions.metrics.dropwizard.DatadogReporterConfig {
            prefix: you-prefix,
            apiKey: you-datadog-api-key,
            mode: http
            }
        ]
      }
```

And then you can start to report metrics in you application:
```
    MetricsManager.getInstance().getRegistry().counter("start_times").inc(1);
```

To Report The Metrics of Orbit Cluster
-----
Extension "OrbitActorExtension" collects metrics related to actor information. "OrbitMessagingMetricsExtension" collects the metrics related the Orbit messaging. 

Modify Orbit configuration to include them if you want to report the metrics of Orbit cluster:
```
      !!cloud.orbit.actors.extensions.metrics.dropwizard.OrbitActorExtension {},

      !!cloud.orbit.actors.extensions.metrics.dropwizard.OrbitMessagingMetricsExtension {}
```