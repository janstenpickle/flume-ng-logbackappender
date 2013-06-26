/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flume.clients.logbackjappender;

public enum LogbackAvroHeaders {
    OTHER("flume.client.logback.logger.other"),
    LOGGER_NAME("flume.client.logback.logger.name"),
    LOG_LEVEL("flume.client.logback.log.level"),
    MESSAGE_ENCODING("flume.client.logback.message.encoding"),
    TIMESTAMP("flume.client.logback.timestamp"),
    AVRO_SCHEMA_LITERAL("flume.avro.schema.literal"),
    AVRO_SCHEMA_URL("flume.avro.schema.url"),
    PREPEND("flume.client.logback.logger"),
    LOGGER_CLASS("flume.client.logback.logger.class"),
    LOG_LEVEL_STRING("flume.client.logback.log.level.string"),
    THREAD("flume.client.logback.thread"),
    THROWABLE("flume.client.logback.throwable"),
    INSTANCE("flume.client.logback.instance"),
    HOST("flume.client.logback.host");

    private String headerName;

    private LogbackAvroHeaders(String headerName) {
        this.headerName = headerName;
    }

    public String getName() {
        return headerName;
    }

    public String toString() {
        return getName();
    }

    public static LogbackAvroHeaders getByName(String headerName) {
        LogbackAvroHeaders hdrs = null;
        try {
            hdrs = LogbackAvroHeaders.valueOf(headerName.toLowerCase().trim());
        } catch (IllegalArgumentException e) {
            hdrs = LogbackAvroHeaders.OTHER;
        }
        return hdrs;
    }

}