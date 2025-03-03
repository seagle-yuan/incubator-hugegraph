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

package org.apache.hugegraph.exception;

import java.util.Map;

import org.apache.hugegraph.util.E;

public class HugeGremlinException extends RuntimeException {

    private static final long serialVersionUID = -8712375282196157058L;

    private final int statusCode;
    private final Map<String, Object> response;

    public HugeGremlinException(int statusCode, Map<String, Object> response) {
        E.checkNotNull(response, "response");
        this.statusCode = statusCode;
        this.response = response;
    }

    public int statusCode() {
        return this.statusCode;
    }

    public Map<String, Object> response() {
        return this.response;
    }
}
