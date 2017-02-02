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
Modify Orbit configuration(orbit.yaml) to include the extension: 
```
cloud.orbit.actors.Stage:
  !!cloud.orbit.actors.Stage
  {
      ...
      !!cloud.orbit.actors.extensions.metrics.dropwizard.MetricsExtension {
        metricsConfig:
        [
          !!cloud.orbit.actors.extensions.metrics.dropwizard.DatadogReporterConfig {
            prefix: you-prefix,
            apiKey: you-datadog-api-key,
            mode: http
            }
        ]
      },
      ...
  } 
```

And then you can start to report metrics in you application:
```
    MetricsManager.getInstance().getRegistry().counter("start_times").inc(1);
```

MetricExtension supports 4 different reporters: JMX, Graphite, Ganglia and Datadog reporter. 
###JMX reporter configuration
| property     | description    | comment |
| --------|---------|-------|
| rateUnit  | Rate time unit   | From NANOSECONDS to DAYS defined in Java TimeUnit class    |
| durationUnit | Duration time unit | From NANOSECONDS to DAYS defined in Java TimeUnit class     |
###Graphite reporter configuration
| property     | description    | comment |
| --------|---------|-------|
| rateUnit  | Rate time unit   | From NANOSECONDS to DAYS defined in Java TimeUnit class    |
| durationUnit | Duration time unit | From NANOSECONDS to DAYS defined in Java TimeUnit class     |
| prefix | prefix for all the metrics | User defined string     |
| host | Graphite server host |      |
| port | Graphite server running port |     |
###Ganglia reporter configuration
| property     | description    | comment |
| --------|---------|-------|
| rateUnit  | Rate time unit   | From NANOSECONDS to DAYS defined in Java TimeUnit class    |
| durationUnit | Duration time unit | From NANOSECONDS to DAYS defined in Java TimeUnit class     |
| prefix | prefix for all the metrics | User defined string     |
| host | Ganglia server host |      |
| port | Ganglia server running port |     |
###Datadog reporter configuration
| property     | description    | comment |
| --------|---------|-------|
| rateUnit  | Rate time unit   | From NANOSECONDS to DAYS defined in Java TimeUnit class    |
| durationUnit | Duration time unit | From NANOSECONDS to DAYS defined in Java TimeUnit class     |
| prefix | prefix for all the metrics | User defined string     |
| mode | Metrics reportingm mode | Can be "udp" or "http"     |
| apiKey | API key of Datadog service | Only apply in "http" mode    |
| statsdHost | StatsD agent host |  Only apply in "udp" mode   |
| statsdPort | StatsD agent port |  Only apply in "udp" mode   |
###Console reporter configuration
| property     | description    | comment |
| --------|---------|-------|
| rateUnit  | Rate time unit   | From NANOSECONDS to DAYS defined in Java TimeUnit class    |
| durationUnit | Duration time unit | From NANOSECONDS to DAYS defined in Java TimeUnit class     |
| output | PrintStream to use for output |  Default: System.out   |

To Report The Metrics of Orbit Cluster
-----
This library also include two optional extensions and handler which can be used to report metrics about Orbit cluster itself. These two extensions and handler depend on "MetricExtension".

Extension "OrbitActorExtension" collects metrics related to actor information. "OrbitMessagingMetricsExtension" collects the metrics related the Orbit messaging. 

Modify Orbit configuration to include them if you want to report the metrics of Orbit cluster:
```
cloud.orbit.actors.Stage:
  !!cloud.orbit.actors.Stage
  {
      ...
      !!cloud.orbit.actors.extensions.metrics.dropwizard.OrbitActorExtension {},

      !!cloud.orbit.actors.extensions.metrics.dropwizard.OrbitMessagingMetricsExtension {},
      ...
  } 
```

Handler - "OrbitMetricsInvocationHandler" reports response time histogram for actors.  Modify Orbit configuration to use it:

```
cloud.orbit.actors.Stage:
  !!cloud.orbit.actors.Stage
  {
    ...
    invocationHandler:
      !!cloud.orbit.actors.extensions.metrics.dropwizard.OrbitMetricsInvocationHandler {}
  } 
 ```
