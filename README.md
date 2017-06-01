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
Allows for the collection of metrics in relation to how an application interacts with Orbit.

Metrics Exposed
======

* OrbitMetricsActorExtension
    * Actor Activation Timer
        * orbit.actors.activation_time[actor:%s]
    * Actor Lifetime Timer
        * orbit.actors.lifetime[actor:%s]
    * Actor Deactivation Timer
        * orbit.actors.deactivation_time[actor:%s]
    * Actor Active Count
        * orbit.actors.count[actor:%s]
    * Actor Message Receive Rate Meter
        * orbit.actors.msg_received_rate[actor:%s]
* OrbitMetricsMessagingExtension
    * Inbound Message Timer per Message Type
        * orbit.messaging[type:%s,direction:inbound]
    * Outbound Message Meter per Message Type
        * orbit.messaging[type:%s,direction:outbound]
* OrbitMetricsInvocationHandler
    * Invocation Timers
        * orbit.actors.methodresponsetime[actor:%s,method:%s]
        * orbit.actors.chainresponsetime[actor:%s,method:%s]
* ExecutionPoolMetrics
    * ForkJoinPool Gauges
        * orbit.stage.executionPool.activeThreadCount
        * orbit.stage.executionPool.poolSize
        * orbit.stage.executionPool.queuedSubmissionCount
        * orbit.stage.executionPool.runningThreadCount
        * orbit.stage.executionPool.queuedTaskCount
        * orbit.stage.executionPool.stealCount
    * ThreadPoolExecutor Gauges
        * orbit.stage.executionPool.activeThreadCount
        * orbit.stage.executionPool.poolSize
        * orbit.stage.executionPool.completedTaskCount
        * orbit.stage.executionPool.largestPoolSize
        * orbit.stage.executionPool.taskCount
        * orbit.stage.executionPool.remainingCapacity

Instructions
======
If no `MetricRegistry` is provided to the `Extension` or `InvocationHandler` then it will construct a `MetricRegistry` and make it available by calling the getter method. Otherwise the provided `MetricRegistry` will be used.

```
MetricRegistry metricRegistry = new MetricRegistry();

OrbitMetricsActorExtension actorExtension = new OrbitMetricsActorExtension(metricRegistry);
OrbitMetricsMessagingExtension messagingExtension = new OrbitMetricsMessagingExtension(metricRegistry);

OrbitMetricsInvocationHandler invocationHandler = new OrbitMetricsInvocationHandler(metricRegistry);

Builder builder = new Stage.Builder();
builder.extensions(actorExtension, messagingExtension);
builder.invocationHandler(invocationHandler);
Stage stage = builder.build();
stage.start();
```

