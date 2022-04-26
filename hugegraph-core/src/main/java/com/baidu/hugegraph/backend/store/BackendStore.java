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

package com.baidu.hugegraph.backend.store;

import java.util.Iterator;
import java.util.Map;

import com.baidu.hugegraph.backend.id.Id;
import com.baidu.hugegraph.backend.id.IdGenerator;
import com.baidu.hugegraph.backend.query.Query;
import com.baidu.hugegraph.config.HugeConfig;
import com.baidu.hugegraph.type.HugeType;
import com.baidu.hugegraph.util.E;

public interface BackendStore {

    // Store name
    String store();

    // Database name
    String database();

    // Get the parent provider
    BackendStoreProvider provider();

    // Whether it is the storage of schema
    boolean isSchemaStore();

    // Open/close database
    void open(HugeConfig config);

    void close();

    boolean opened();

    // Initialize/clear database
    void init();

    void clear(boolean clearSpace);

    boolean initialized();

    // Delete all data of database (keep table structure)
    void truncate();

    // Add/delete data
    void mutate(BackendMutation mutation);

    // Query data
    Iterator<BackendEntry> query(Query query);

    Number queryNumber(Query query);

    // Transaction
    void beginTx();

    void commitTx();

    void rollbackTx();

    // Get metadata by key
    <R> R metadata(HugeType type, String meta, Object[] args);

    // Backend features
    BackendFeatures features();

    // Generate an id for a specific type
    default Id nextId(HugeType type) {
        final int MAX_TIMES = 1000;
        // Do get-increase-get-compare operation
        long counter = 0L;
        long expect = -1L;
        synchronized(this) {
            for (int i = 0; i < MAX_TIMES; i++) {
                counter = this.getCounter(type);

                if (counter == expect) {
                    break;
                }
                // Increase local counter
                expect = counter + 1L;
                // Increase remote counter
                this.increaseCounter(type, 1L);
            }
        }

        E.checkState(counter != 0L, "Please check whether '%s' is OK",
                     this.provider().type());

        E.checkState(counter == expect, "'%s' is busy please try again",
                     this.provider().type());

        return IdGenerator.of(expect);
    }

    // Set next id >= lowest for a specific type
    default void setCounterLowest(HugeType type, long lowest) {
        long current = this.getCounter(type);
        if (current >= lowest) {
            return;
        }
        long increment = lowest - current;
        this.increaseCounter(type, increment);
    }

    default String olapTableName(HugeType type) {
        StringBuilder sb = new StringBuilder(7);
        sb.append(this.store())
          .append("_")
          .append(HugeType.OLAP.string())
          .append("_")
          .append(type.string());
        return sb.toString().toLowerCase();
    }

    default String olapTableName(Id id) {
        StringBuilder sb = new StringBuilder(5 + 4);
        sb.append(this.store())
          .append("_")
          .append(HugeType.OLAP.string())
          .append("_")
          .append(id.asLong());
        return sb.toString().toLowerCase();
    }

    // Increase next id for specific type
    void increaseCounter(HugeType type, long increment);

    // Get current counter for a specific type
    long getCounter(HugeType type);

    default void createOlapTable(Id pkId) {
        throw new UnsupportedOperationException(
                  "BackendStore.createOlapTable()");
    }

    default void checkAndRegisterOlapTable(Id pkId) {
        throw new UnsupportedOperationException(
                  "BackendStore.checkAndRegisterOlapTable()");
    }

    default void clearOlapTable(Id pkId) {
        throw new UnsupportedOperationException(
                  "BackendStore.clearOlapTable()");
    }

    default void removeOlapTable(Id pkId) {
        throw new UnsupportedOperationException(
                  "BackendStore.removeOlapTable()");
    }

    default Map<String, String> createSnapshot(String snapshotDir) {
        throw new UnsupportedOperationException("createSnapshot");
    }

    default void resumeSnapshot(String snapshotDir,
                                       boolean deleteSnapshot) {
        throw new UnsupportedOperationException("resumeSnapshot");
    }

    enum TxState {
        BEGIN, COMMITTING, COMMITT_FAIL, ROLLBACKING, ROLLBACK_FAIL, CLEAN
    }
}
