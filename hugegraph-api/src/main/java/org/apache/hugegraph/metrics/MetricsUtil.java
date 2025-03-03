/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.metrics;

import org.apache.tinkerpop.gremlin.server.util.MetricManager;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricsUtil {

    private static final MetricRegistry REGISTRY =
                                        MetricManager.INSTANCE.getRegistry();

    public static <T> Gauge<T> registerGauge(Class<?> clazz, String name,
                                             Gauge<T> gauge) {
        return REGISTRY.register(MetricRegistry.name(clazz, name), gauge);
    }

    public static Counter registerCounter(Class<?> clazz, String name) {
        return REGISTRY.counter(MetricRegistry.name(clazz, name));
    }

    public static Histogram registerHistogram(Class<?> clazz, String name) {
        return REGISTRY.histogram(MetricRegistry.name(clazz, name));
    }

    public static Meter registerMeter(Class<?> clazz, String name) {
        return REGISTRY.meter(MetricRegistry.name(clazz, name));
    }

    public static Timer registerTimer(Class<?> clazz, String name) {
        return REGISTRY.timer(MetricRegistry.name(clazz, name));
    }
}
