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

package org.apache.hugegraph.backend.store.postgresql;

import static org.apache.hugegraph.backend.store.mysql.MysqlTables.BOOLEAN;
import static org.apache.hugegraph.backend.store.mysql.MysqlTables.HUGE_TEXT;
import static org.apache.hugegraph.backend.store.mysql.MysqlTables.INT;
import static org.apache.hugegraph.backend.store.mysql.MysqlTables.LARGE_TEXT;
import static org.apache.hugegraph.backend.store.mysql.MysqlTables.MID_TEXT;
import static org.apache.hugegraph.backend.store.mysql.MysqlTables.NUMERIC;
import static org.apache.hugegraph.backend.store.mysql.MysqlTables.SMALL_TEXT;
import static org.apache.hugegraph.backend.store.mysql.MysqlTables.TINYINT;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.backend.BackendException;
import org.apache.hugegraph.backend.id.Id;
import org.apache.hugegraph.backend.store.BackendEntry;
import org.apache.hugegraph.backend.store.TableDefine;
import org.apache.hugegraph.backend.store.mysql.MysqlBackendEntry;
import org.apache.hugegraph.backend.store.mysql.MysqlSessions.Session;
import org.apache.hugegraph.backend.store.mysql.MysqlTables;
import org.apache.hugegraph.backend.store.mysql.MysqlTables.MysqlTableTemplate;
import org.apache.hugegraph.type.HugeType;
import org.apache.hugegraph.type.define.Directions;
import org.apache.hugegraph.type.define.HugeKeys;

import com.google.common.collect.ImmutableMap;

public class PostgresqlTables {

    private static final Map<String, String> TYPES_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(BOOLEAN, "BOOL")
                    .put(TINYINT, "INT")
                    .put(INT, "INT")
                    .put(NUMERIC, "DECIMAL")
                    .put(SMALL_TEXT, "VARCHAR(255)")
                    .put(MID_TEXT, "VARCHAR(1024)")
                    .put(LARGE_TEXT, "VARCHAR(65533)")
                    .put(HUGE_TEXT, "TEXT")
                    .build();

    public static class PostgresqlTableTemplate extends PostgresqlTable {

        protected MysqlTableTemplate template;

        public PostgresqlTableTemplate(MysqlTableTemplate template) {
            super(template.table());
            this.template = template;
        }

        @Override
        public TableDefine tableDefine() {
            return this.template.tableDefine();
        }
    }

    public static class Meta extends PostgresqlTableTemplate {

        public Meta() {
            super(new MysqlTables.Meta(TYPES_MAPPING));
        }

        public void writeVersion(Session session, String version) {
            String versionColumn = formatKey(HugeKeys.VERSION);
            String insert = String.format("INSERT INTO %s VALUES ('%s', '%s') " +
                                          "ON CONFLICT(name) DO NOTHING;",
                                          this.table(), versionColumn, version);
            try {
                session.execute(insert);
            } catch (SQLException e) {
                throw new BackendException("Failed to insert driver version " +
                                           "with '%s'", e, insert);
            }
        }

        public String readVersion(Session session) {
            MysqlTables.Meta table = (MysqlTables.Meta) this.template;
            return table.readVersion(session);
        }
    }

    public static class Counters extends PostgresqlTableTemplate {

        public Counters() {
            super(new MysqlTables.Counters(TYPES_MAPPING));
        }

        public long getCounter(Session session, HugeType type) {
            MysqlTables.Counters table = (MysqlTables.Counters) this.template;
            return table.getCounter(session, type);
        }

        public void increaseCounter(Session session, HugeType type,
                                    long increment) {
            String update = String.format(
                            "INSERT INTO %s (%s, %s) VALUES ('%s', %s) " +
                            "ON CONFLICT (%s) DO UPDATE SET ID = %s.ID + %s;",
                            this.table(), formatKey(HugeKeys.SCHEMA_TYPE),
                            formatKey(HugeKeys.ID), type.name(), increment,
                            formatKey(HugeKeys.SCHEMA_TYPE),
                            this.table(), increment);
            try {
                session.execute(update);
            } catch (SQLException e) {
                throw new BackendException(
                          "Failed to update counters with type '%s'", e, type);
            }
        }
    }

    public static class VertexLabel extends PostgresqlTableTemplate {

        public VertexLabel() {
            super(new MysqlTables.VertexLabel(TYPES_MAPPING));
        }
    }

    public static class EdgeLabel extends PostgresqlTableTemplate {

        public EdgeLabel() {
            super(new MysqlTables.EdgeLabel(TYPES_MAPPING));
        }
    }

    public static class PropertyKey extends PostgresqlTableTemplate {

        public PropertyKey() {
            super(new MysqlTables.PropertyKey(TYPES_MAPPING));
        }
    }

    public static class IndexLabel extends PostgresqlTableTemplate {

        public IndexLabel() {
            super(new MysqlTables.IndexLabel(TYPES_MAPPING));
        }
    }

    public static class Vertex extends PostgresqlTableTemplate {

        public static final String TABLE = HugeType.VERTEX.string();

        public Vertex(String store) {
            super(new MysqlTables.Vertex(store, TYPES_MAPPING));
        }
    }

    public static class Edge extends PostgresqlTableTemplate {

        public Edge(String store, Directions direction) {
            super(new MysqlTables.Edge(store, direction, TYPES_MAPPING));
        }

        @Override
        protected List<Object> idColumnValue(Id id) {
            MysqlTables.Edge table = (MysqlTables.Edge) this.template;
            return table.idColumnValue(id);
        }

        @Override
        public void delete(Session session, MysqlBackendEntry.Row entry) {
            MysqlTables.Edge table = (MysqlTables.Edge) this.template;
            table.delete(session, entry);
        }

        @Override
        protected BackendEntry mergeEntries(BackendEntry e1, BackendEntry e2) {
            MysqlTables.Edge table = (MysqlTables.Edge) this.template;
            return table.mergeEntries(e1, e2);
        }
    }

    public static class SecondaryIndex extends PostgresqlTableTemplate {

        public static final String TABLE = MysqlTables.SecondaryIndex.TABLE;

        public SecondaryIndex(String store) {
            super(new MysqlTables.SecondaryIndex(store, TABLE, TYPES_MAPPING));
        }

        public SecondaryIndex(String store, String table) {
            super(new MysqlTables.SecondaryIndex(store, table, TYPES_MAPPING));
        }

        protected final String entryId(MysqlBackendEntry entry) {
            return ((MysqlTables.SecondaryIndex) this.template).entryId(entry);
        }
    }

    public static class SearchIndex extends SecondaryIndex {

        public static final String TABLE = MysqlTables.SearchIndex.TABLE;

        public SearchIndex(String store) {
            super(store, TABLE);
        }
    }

    public static class UniqueIndex extends SecondaryIndex {

        public static final String TABLE = MysqlTables.UniqueIndex.TABLE;

        public UniqueIndex(String store) {
            super(store, TABLE);
        }
    }

    public static class RangeIntIndex extends PostgresqlTableTemplate {

        public static final String TABLE = HugeType.RANGE_INT_INDEX.string();

        public RangeIntIndex(String store) {
            super(new MysqlTables.RangeIntIndex(store, TABLE, TYPES_MAPPING));
        }

        protected final String entryId(MysqlBackendEntry entry) {
            return ((MysqlTables.RangeIntIndex) this.template).entryId(entry);
        }
    }

    public static class RangeFloatIndex extends PostgresqlTableTemplate {

        public static final String TABLE = HugeType.RANGE_FLOAT_INDEX.string();

        public RangeFloatIndex(String store) {
            super(new MysqlTables.RangeFloatIndex(store, TABLE, TYPES_MAPPING));
        }

        protected final String entryId(MysqlBackendEntry entry) {
            return ((MysqlTables.RangeFloatIndex) this.template).entryId(entry);
        }
    }

    public static class RangeLongIndex extends PostgresqlTableTemplate {

        public static final String TABLE = HugeType.RANGE_LONG_INDEX.string();

        public RangeLongIndex(String store) {
            super(new MysqlTables.RangeLongIndex(store, TABLE, TYPES_MAPPING));
        }

        protected final String entryId(MysqlBackendEntry entry) {
            return ((MysqlTables.RangeLongIndex) this.template).entryId(entry);
        }
    }

    public static class RangeDoubleIndex extends PostgresqlTableTemplate {

        public static final String TABLE = HugeType.RANGE_DOUBLE_INDEX.string();

        public RangeDoubleIndex(String store) {
            super(new MysqlTables.RangeDoubleIndex(store, TABLE,
                                                   TYPES_MAPPING));
        }

        protected final String entryId(MysqlBackendEntry entry) {
            return ((MysqlTables.RangeDoubleIndex) this.template)
                   .entryId(entry);
        }
    }

    public static class ShardIndex extends PostgresqlTableTemplate {

        public ShardIndex(String store) {
            super(new MysqlTables.ShardIndex(store, TYPES_MAPPING));
        }

        protected final String entryId(MysqlBackendEntry entry) {
            return ((MysqlTables.ShardIndex) this.template).entryId(entry);
        }
    }
}
