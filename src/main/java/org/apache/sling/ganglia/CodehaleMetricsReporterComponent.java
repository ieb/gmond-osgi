/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.ganglia;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ganglia.GangliaReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
import org.apache.felix.scr.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A Dropwizard metrics reported that reports the Oak metrics, if there is a metricsStatisticsProvider service present.
 * If there is no MetricsStatisticsProvider this component doesnt activate and does nothing.
 * Since it will report all Dropwizard Metrics directly without configuration there is no need to have the same
 * metrics shipped from metrics -> JMX -> JMXMetrics Reporter -> Ganglia.
 */
@Component(immediate = true , metatype = true)
@References(value = {@Reference(
        referenceInterface = MetricRegistry.class,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "bindMetricRegistry",
        unbind = "unbindMetricRegistry")}
)
public class CodehaleMetricsReporterComponent {

    private static final Logger LOG = LoggerFactory.getLogger(CodehaleMetricsReporterComponent.class);

    private GangliaReporter reporter;

    @Property(value = "13.94.149.199", description = "the multicast or unicast address that is used to publish metrics to the ganglia gmond process")
    public static final String GMOND_HOST = "host";
    @Property(intValue = 8649, description = "the multicast group or unicast port that is used to publish metrics to the ganglia gmond process.")
    public static final String GMOND_PORT = "port";
    @Property(intValue = 1, description = "the ttl for UDP packets.")
    @Property(intValue = 5, description = "The period in seconds the reporter reports at")
    public static final String REPORT_PERIOD = "period";
    public static final String GMOND_TTL = "ttl";
    @Property(boolValue = false, description = "If true, multicast, if false unicast")
    public static final String GMOND_MULTICAST = "multicast";
    @Property(boolValue = true, description = "If true, use 3.1.x wire format, if false use earler wire format.")
    public static final String GMOND_WIRE_FORMAT_31X = "wireformat31x";
    @Property(value = "", description = "An IP:hostname pair that will be used to spoof the metric host information..")
    public static final String GMOND_SPOOF = "spoof";
    private java.util.UUID uuid = UUID.randomUUID();
    private ConcurrentMap<String, CopyMetricRegistryListener> listeners = new ConcurrentHashMap<String, CopyMetricRegistryListener>();
    private MetricRegistry metricRegistry = new MetricRegistry();

    @Activate
    public void acivate(Map<String, Object> properties) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        LOG.info("Starting Ganglia Metrics ");
        String host = (String) properties.get(GMOND_HOST);
        int port = (int) properties.get(GMOND_PORT);
        int period = (int) properties.get(REPORT_PERIOD);
        boolean multicast = (boolean) properties.get(GMOND_MULTICAST);
        boolean use311Wire  = (boolean) properties.get(GMOND_WIRE_FORMAT_31X);
        String spoof = (String) properties.get(GMOND_SPOOF);
        int ttl = (int) properties.get(GMOND_TTL);
        final GMetric ganglia = new GMetric(host, port,
                multicast?GMetric.UDPAddressingMode.MULTICAST: GMetric.UDPAddressingMode.UNICAST,
                ttl, use311Wire);
        reporter = GangliaReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(ganglia);
        reporter.start(period, TimeUnit.SECONDS);
        LOG.info("Started Ganglia Metrics reporter to {}:{} {} {} ttl:{} ", new Object[]{host, port, multicast ? "multicast" : "unicast", use311Wire ? "311" : "pre311", ttl});
    }

    @Deactivate
    public void deacivate(Map<String, Object> properties) {
        reporter.stop();
        reporter = null;
    }

    protected void bindMetricRegistry(MetricRegistry metricRegistry, Map<String, Object> properties) {
        String name = (String) properties.get("name");
        if (name == null) {
            name = metricRegistry.toString();
        }
        CopyMetricRegistryListener listener = new CopyMetricRegistryListener(this.metricRegistry, name);
        listener.start(metricRegistry);
        this.listeners.put(name, listener);
        LOG.info("Bound Metrics Registry {} ",name);
    }
    protected void unbindMetricRegistry(MetricRegistry metricRegistry, Map<String, Object> properties) {
        String name = (String) properties.get("name");
        if (name == null) {
            name = metricRegistry.toString();
        }
        CopyMetricRegistryListener metricRegistryListener = listeners.get(name);
        if ( metricRegistryListener != null) {
            metricRegistryListener.stop(metricRegistry);
            this.listeners.remove(name);
        }
        LOG.info("Unbound Metrics Registry {} ",name);
    }


}
