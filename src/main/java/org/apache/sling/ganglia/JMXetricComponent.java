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

import info.ganglia.jmxetric.JMXetricAgent;
import info.ganglia.jmxetric.XMLConfigurationService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An OSGi Component wrapping a JMXetricComponent.
 *
 */

@Component(metatype = true, immediate = true)
public class JMXetricComponent {

    @Property(value = "127.0.0.1", description = "the multicast or unicast address that is used to publish metrics to the ganglia gmond process")
    public static final String GMOND_HOST = "host";
    @Property(intValue = 8649, description = "the multicast group or unicast port that is used to publish metrics to the ganglia gmond process.")
    public static final String GMOND_PORT = "port";
    @Property(boolValue = false, description = "If true, multicast, if false unicast")
    public static final String GMOND_MULTICAST = "multicast";
    @Property(boolValue = true, description = "If true, use 3.1.x wire format, if false use earler wire format.")
    public static final String GMOND_WIRE_FORMAT_31X = "wireformat31x";
    @Property(value = "", description = "An IP:hostname pair that will be used to spoof the metric host information..")
    public static final String GMOND_SPOOF = "spoof";
    @Property(value = "jvm", description = "Name of the metrics group.")
    public static final String GMOND_PROCESS = "process";
    @Property(value = "jmxetric.xml", description = "The full path to the config file (jmxetric.xml)")
    public static final String GMOND_CONFIG = "config";
    private static final Logger LOG = LoggerFactory.getLogger(JMXetricComponent.class);


    private JMXetricAgent agent;
    private ConfigWatcher configWatcher;

    public interface ConfigWatcherListener {
        void onChange();
    }

    public class ConfigWatcher extends Timer {
        private long lastUpdate;

        public ConfigWatcher(String config, final ConfigWatcherListener listener) {
            this.lastUpdate = 0;
            final File configFile = new File(config);
            if ( ! configFile.exists()) {
                LOG.warn("Config file {} does not exist. Waiting for it to appear.", configFile.getAbsolutePath());
            } else {
                LOG.info("Watching config file {}.", configFile.getAbsolutePath());
            }
            this.schedule(new TimerTask() {
                @Override
                public void run() {
                    LOG.debug("Checking Config file {} ", configFile);
                    if ( configFile.exists() && configFile.lastModified() > lastUpdate ) {
                        LOG.info("Config file {} changed, will reconfigure agent.", configFile.getAbsolutePath());
                        lastUpdate = configFile.lastModified();
                        listener.onChange();
                    }
                }
            }, 100, 10000);

        }
    }

    @Activate
    public void activate(Map<String, Object> config) {
        final String agentArgs = buildAgentArgs(config);
        configWatcher = new ConfigWatcher((String) config.get(GMOND_CONFIG), new ConfigWatcherListener() {
            @Override
            public void onChange() {
                LOG.info("Starting GMonitor using args ", agentArgs);
                try {
                    stopAgent();
                    agent = new JMXetricAgent();
                    XMLConfigurationService.configure(agent, agentArgs);
                    agent.start();
                } catch ( Exception ex ) {
                    LOG.error("Unable to start GMonitor Agent. ",ex);
                }
            }
        });
    }

    private void stopAgent() {
        if ( agent != null ) {
            agent.stop();
        }
        agent = null;
    }

    @Deactivate
    public void deactivate(Map<String, Object> config) {
        configWatcher.cancel();
        stopAgent();
    }



    private String buildAgentArgs(Map<String, Object> config) {
        StringBuilder sb = new StringBuilder();
        sb.append("config=").append(config.get(GMOND_CONFIG)).append(",");
        sb.append("host=").append(config.get(GMOND_HOST)).append(",");
        sb.append("port=").append(config.get(GMOND_PORT)).append(",");
        sb.append("mode=").append(select((Boolean) config.get(GMOND_MULTICAST), "multicast", "unicast")).append(",");
        sb.append("wireformat31x=").append(select((Boolean) config.get(GMOND_WIRE_FORMAT_31X), "true", "false")).append(",");
        String spoof = (String) config.get(GMOND_SPOOF);
        if ( spoof != null && spoof.trim().length() > 0) {
            sb.append("spoof=").append(spoof).append(",");
        }
        sb.append("process=").append(config.get(GMOND_PROCESS));
        return sb.toString();
    }

    private String select(Boolean bool, String trueValue, String falseValue) {
        if ( bool != null && bool.booleanValue()) {
            return trueValue;
        }
        return falseValue;
    }



}
