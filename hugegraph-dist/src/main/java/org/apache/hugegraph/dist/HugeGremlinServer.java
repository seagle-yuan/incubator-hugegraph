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

package org.apache.hugegraph.dist;

import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.slf4j.Logger;

import org.apache.hugegraph.HugeException;
import org.apache.hugegraph.auth.ContextGremlinServer;
import org.apache.hugegraph.event.EventHub;
import org.apache.hugegraph.util.ConfigUtil;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.Log;

public class HugeGremlinServer {

    private static final Logger LOG = Log.logger(HugeGremlinServer.class);

    public static GremlinServer start(String conf, String graphsDir,
                                      EventHub hub) throws Exception {
        // Start GremlinServer with inject traversal source
        LOG.info(GremlinServer.getHeader());
        final Settings settings;
        try {
            settings = Settings.read(conf);
        } catch (Exception e) {
            LOG.error("Can't found the configuration file at '{}' or " +
                      "being parsed properly. [{}]", conf, e.getMessage());
            throw e;
        }
        // Scan graph confs and inject into gremlin server context
        E.checkState(settings.graphs != null,
                     "The GremlinServer's settings.graphs is null");
        settings.graphs.putAll(ConfigUtil.scanGraphsDir(graphsDir));

        LOG.info("Configuring Gremlin Server from {}", conf);
        ContextGremlinServer server = new ContextGremlinServer(settings, hub);

        // Inject customized traversal source
        server.injectTraversalSource();

        server.start().exceptionally(t -> {
            LOG.error("Gremlin Server was unable to start and will " +
                      "shutdown now: {}", t.getMessage());
            server.stop().join();
            throw new HugeException("Failed to start Gremlin Server");
        }).join();

        return server;
    }
}
