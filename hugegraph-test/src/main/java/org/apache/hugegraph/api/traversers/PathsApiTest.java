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

package org.apache.hugegraph.api.traversers;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.hugegraph.api.BaseApiTest;

import com.google.common.collect.ImmutableMap;

public class PathsApiTest extends BaseApiTest {

    static final String PATH = TRAVERSERS_API + "/paths";

    @Before
    public void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initVertex();
        BaseApiTest.initEdge();
    }

    @Test
    public void testGet() {
        Map<String, String> name2Ids = listAllVertexName2Ids();
        String markoId = name2Ids.get("marko");
        String vadasId = name2Ids.get("vadas");
        Response r = client().get(PATH, ImmutableMap.of("source",
                                                        id2Json(markoId),
                                                        "target",
                                                        id2Json(vadasId),
                                                        "max_depth", 3));
        String content = assertResponseStatus(200, r);
        assertJsonContains(content, "paths");
        List<Map<String, Object>> paths = assertJsonContains(content, "paths");
        Assert.assertEquals(1, paths.size());
    }

    @Test
    public void testPost() {
        Map<String, String> name2Ids = listAllVertexName2Ids();
        String markoId = name2Ids.get("marko");
        String joshId = name2Ids.get("josh");
        String reqBody = String.format("{ " +
                                       "\"sources\": { " +
                                       " \"ids\": [\"%s\"]}, " +
                                       "\"targets\": { " +
                                       " \"ids\": [\"%s\"]}, " +
                                       "\"step\": { " +
                                       " \"direction\": \"BOTH\", " +
                                       " \"properties\": { " +
                                       "  \"weight\": \"P.gt(0.01)\"}}, " +
                                       "\"max_depth\": 10, " +
                                       "\"capacity\": 100000000, " +
                                       "\"limit\": 10000000, " +
                                       "\"with_vertex\": false}",
                                       markoId, joshId);
        Response r = client().post(PATH, reqBody);
        String content = assertResponseStatus(200, r);
        List<Map<String, Object>> paths = assertJsonContains(content, "paths");
        Assert.assertEquals(2, paths.size());
    }
}
