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

package org.apache.hugegraph.backend.store.mysql;

import org.apache.hugegraph.backend.store.AbstractBackendStoreProvider;
import org.apache.hugegraph.backend.store.BackendStore;
import org.apache.hugegraph.backend.store.mysql.MysqlStore.MysqlGraphStore;
import org.apache.hugegraph.backend.store.mysql.MysqlStore.MysqlSchemaStore;
import org.apache.hugegraph.backend.store.mysql.MysqlStore.MysqlSystemStore;
import org.apache.hugegraph.config.HugeConfig;

public class MysqlStoreProvider extends AbstractBackendStoreProvider {

    protected String database() {
        return this.graph().toLowerCase();
    }

    @Override
    protected BackendStore newSchemaStore(HugeConfig config, String store) {
        return new MysqlSchemaStore(this, this.database(), store);
    }

    @Override
    protected BackendStore newGraphStore(HugeConfig config, String store) {
        return new MysqlGraphStore(this, this.database(), store);
    }

    @Override
    protected BackendStore newSystemStore(HugeConfig config, String store) {
        return new MysqlSystemStore(this, this.database(), store);
    }

    @Override
    public String type() {
        return "mysql";
    }

    @Override
    public String driverVersion() {
        /*
         * Versions history:
         * [1.0] HugeGraph-1328: supports backend table version checking
         * [1.1] HugeGraph-1322: add support for full-text search
         * [1.2] #296: support range sortKey feature
         * [1.3] #270 & #398: support shard-index and vertex + sortkey prefix,
         *                    also split range table to rangeInt, rangeFloat,
         *                    rangeLong and rangeDouble
         * [1.4] #633: support unique index
         * [1.5] #661: reduce the storage of vertex/edge id
         * [1.6] #691: support aggregate property
         * [1.7] #746: support userdata for indexlabel
         * [1.8] #894: asStoredString() encoding is changed to signed B64
         *             instead of sortable B64
         * [1.9] #295: support ttl for vertex and edge
         * [1.10] #1333: support read frequency for property key
         * [1.11] #1506: rename read frequency to write type
         * [1.11] #1533: add meta table in system store
         */
        return "1.11";
    }
}
