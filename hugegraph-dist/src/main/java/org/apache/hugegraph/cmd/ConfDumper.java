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

package org.apache.hugegraph.cmd;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.dist.RegisterUtil;
import org.slf4j.Logger;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.config.OptionSpace;
import org.apache.hugegraph.config.TypedOption;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.Log;

public class ConfDumper {

    public static final String EOL = System.getProperty("line.separator");

    private static final Logger LOG = Log.logger(ConfDumper.class);

    public static void main(String[] args)
                       throws ConfigurationException, IOException {
        E.checkArgument(args.length == 1,
                        "ConfDumper need a config file.");

        String input = args[0];
        File output = new File(input + ".default");

        LOG.info("Input config: {}", input);
        LOG.info("Output config: {}", output.getPath());

        RegisterUtil.registerBackends();
        RegisterUtil.registerServer();

        HugeConfig config = new HugeConfig(input);

        for (String name : new TreeSet<>(OptionSpace.keys())) {
            TypedOption<?, ?> option = OptionSpace.get(name);
            writeOption(output, option, config.get(option));
        }
    }

    private static void writeOption(File output, TypedOption<?, ?> option,
                                    Object value) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(option.desc()).append(EOL);
        sb.append(option.name()).append("=").append(value).append(EOL);
        sb.append(EOL);
        // Write to output file
        FileUtils.write(output, sb.toString(), true);
    }
}
