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

package org.apache.hugegraph.backend.store.rocksdb;

import org.apache.hugegraph.backend.store.BackendFeatures;

public class RocksDBFeatures implements BackendFeatures {

    @Override
    public boolean supportsSharedStorage() {
        return false;
    }

    @Override
    public boolean supportsSnapshot() {
        return true;
    }

    @Override
    public boolean supportsScanToken() {
        return false;
    }

    @Override
    public boolean supportsScanKeyPrefix() {
        return true;
    }

    @Override
    public boolean supportsScanKeyRange() {
        return true;
    }

    @Override
    public boolean supportsQuerySchemaByName() {
        // No index in RocksDB
        return false;
    }

    @Override
    public boolean supportsQueryByLabel() {
        // No index in RocksDB
        return false;
    }

    @Override
    public boolean supportsQueryWithInCondition() {
        return false;
    }

    @Override
    public boolean supportsQueryWithRangeCondition() {
        return true;
    }

    @Override
    public boolean supportsQueryWithOrderBy() {
        return true;
    }

    @Override
    public boolean supportsQueryWithContains() {
        // TODO: Need to traversal all items
        return false;
    }

    @Override
    public boolean supportsQueryWithContainsKey() {
        // TODO: Need to traversal all items
        return false;
    }

    @Override
    public boolean supportsQueryByPage() {
        return true;
    }

    @Override
    public boolean supportsQuerySortByInputIds() {
        return true;
    }

    @Override
    public boolean supportsDeleteEdgeByLabel() {
        // No index in RocksDB
        return false;
    }

    @Override
    public boolean supportsUpdateVertexProperty() {
        // Vertex properties are stored in a cell(column value)
        return false;
    }

    @Override
    public boolean supportsMergeVertexProperty() {
        return false;
    }

    @Override
    public boolean supportsUpdateEdgeProperty() {
        // Edge properties are stored in a cell(column value)
        return false;
    }

    @Override
    public boolean supportsTransaction() {
        // Supports tx with WriteBatch
        return true;
    }

    @Override
    public boolean supportsNumberType() {
        return false;
    }

    @Override
    public boolean supportsAggregateProperty() {
        return false;
    }

    @Override
    public boolean supportsTtl() {
        return false;
    }

    @Override
    public boolean supportsOlapProperties() {
        return true;
    }
}
