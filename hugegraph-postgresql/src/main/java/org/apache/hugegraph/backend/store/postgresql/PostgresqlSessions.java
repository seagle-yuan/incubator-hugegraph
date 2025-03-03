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

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.http.client.utils.URIBuilder;
import org.postgresql.core.Utils;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;

import org.apache.hugegraph.backend.BackendException;
import org.apache.hugegraph.backend.store.mysql.MysqlSessions;
import org.apache.hugegraph.backend.store.mysql.MysqlUtil;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.util.Log;

public class PostgresqlSessions extends MysqlSessions {

    private static final Logger LOG = Log.logger(PostgresqlSessions.class);

    private static final String COCKROACH_DB_CREATE =
            "CREATE DATABASE %s ENCODING='UTF-8'";
    private static final String POSTGRESQL_DB_CREATE = COCKROACH_DB_CREATE +
            " TEMPLATE=template0 LC_COLLATE='C' LC_CTYPE='C';";

    public PostgresqlSessions(HugeConfig config, String database, String store) {
        super(config, database, store);
    }

    @Override
    public boolean existsDatabase() {
        String statement = String.format(
                           "SELECT datname FROM pg_catalog.pg_database " +
                           "WHERE datname = '%s';", this.escapedDatabase());
        try (Connection conn = this.openWithoutDB(0)) {
            ResultSet result = conn.createStatement().executeQuery(statement);
            return result.next();
        } catch (Exception e) {
            throw new BackendException("Failed to obtain database info", e);
        }
    }

    @Override
    public void createDatabase() {
        // Create database with non-database-session
        LOG.debug("Create database: {}", this.database());

        String sql = this.buildCreateDatabase(this.database());
        try (Connection conn = this.openWithoutDB(0)) {
            try {
                conn.createStatement().execute(sql);
            } catch (PSQLException e) {
                // CockroachDB not support 'template' arg of CREATE DATABASE
                if (e.getMessage().contains("syntax error at or near " +
                                            "\"template\"")) {
                    sql = String.format(COCKROACH_DB_CREATE, this.database());
                    conn.createStatement().execute(sql);
                }
            }
        } catch (SQLException e) {
            if (!e.getMessage().endsWith("already exists")) {
                throw new BackendException("Failed to create database '%s'", e,
                                           this.database());
            }
            // Ignore exception if database already exists
        }
    }

    @Override
    protected String buildCreateDatabase(String database) {
        return String.format(POSTGRESQL_DB_CREATE, database);
    }

    @Override
    protected String buildDropDatabase(String database) {
        return String.format(
               "REVOKE CONNECT ON DATABASE %s FROM public;" +
               "SELECT pg_terminate_backend(pg_stat_activity.pid) " +
               "  FROM pg_stat_activity " +
               "  WHERE pg_stat_activity.datname = %s;" +
               "DROP DATABASE IF EXISTS %s;",
               database, escapeAndWrapString(database), database);
    }

    @Override
    protected String buildExistsTable(String table) {
        return String.format(
               "SELECT * FROM information_schema.tables " +
               "WHERE table_schema = 'public' AND table_name = '%s' LIMIT 1;",
               MysqlUtil.escapeString(table));
    }

    @Override
    protected URIBuilder newConnectionURIBuilder(String url)
                                                 throws URISyntaxException {
        // Suppress error log when database does not exist
        return new URIBuilder(url).addParameter("loggerLevel", "OFF");
    }

    @Override
    protected String connectDatabase() {
        return this.config().get(PostgresqlOptions.POSTGRESQL_CONNECT_DATABASE);
    }

    public static String escapeAndWrapString(String value) {
        StringBuilder builder = new StringBuilder(8 + value.length());
        builder.append('\'');
        try {
            Utils.escapeLiteral(builder, value, false);
        } catch (SQLException e) {
            throw new BackendException("Failed to escape '%s'", e, value);
        }
        builder.append('\'');
        return builder.toString();
    }
}
