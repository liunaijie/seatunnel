/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.format.json.debezium;

import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;

import org.apache.seatunnel.api.serialization.DeserializationErrorHandleWay;
import org.apache.seatunnel.api.serialization.DeserializationException;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.TablePath;
import org.apache.seatunnel.api.table.type.RowKind;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.format.json.JsonDeserializationSchema;
import org.apache.seatunnel.format.json.exception.JsonErrorCode;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;

public class DebeziumJsonDeserializationSchema implements DeserializationSchema<SeaTunnelRow> {
    private static final long serialVersionUID = 1L;

    private static final String OP_KEY = "op";
    private static final String OP_READ = "r"; // snapshot read
    private static final String OP_CREATE = "c"; // insert
    private static final String OP_UPDATE = "u"; // update
    private static final String OP_DELETE = "d"; // delete
    public static final String DATA_PAYLOAD = "payload";
    private static final String DATA_BEFORE = "before";
    private static final String DATA_AFTER = "after";

    private static final String REPLICA_IDENTITY_EXCEPTION =
            "The \"before\" field of %s operation is null, "
                    + "if you are using Debezium Postgres Connector, "
                    + "please check the Postgres table has been set REPLICA IDENTITY to FULL level.";

    private final SeaTunnelRowType rowType;

    private final JsonDeserializationSchema jsonDeserializer;

    private final DebeziumRowConverter debeziumRowConverter;

    private final DeserializationErrorHandleWay errorHandleWay;

    private final boolean debeziumEnabledSchema;

    private final TablePath tablePath;

    public DebeziumJsonDeserializationSchema(
            CatalogTable catalogTable, DeserializationErrorHandleWay errorHandleWay) {
        this(catalogTable, errorHandleWay, false);
    }

    public DebeziumJsonDeserializationSchema(
            CatalogTable catalogTable,
            DeserializationErrorHandleWay errorHandleWay,
            boolean debeziumEnabledSchema) {
        this.rowType = catalogTable.getSeaTunnelRowType();
        this.errorHandleWay = errorHandleWay;
        this.jsonDeserializer = new JsonDeserializationSchema(catalogTable, errorHandleWay);
        this.debeziumRowConverter = new DebeziumRowConverter(rowType);
        this.debeziumEnabledSchema = debeziumEnabledSchema;
        this.tablePath = Optional.of(catalogTable).map(CatalogTable::getTablePath).orElse(null);
    }

    @Override
    public SeaTunnelRow deserialize(byte[] message) throws DeserializationException {
        throw new UnsupportedOperationException(
                "Please invoke DeserializationSchema#deserialize(byte[], Collector<SeaTunnelRow>) instead.");
    }

    @Override
    public void deserialize(byte[] message, Collector<SeaTunnelRow> out) {
        deserializeMessage(message, out, tablePath);
    }

    public void deserializeMessage(
            byte[] message, Collector<SeaTunnelRow> out, TablePath tablePath) {
        if (message == null || message.length == 0) {
            // skip tombstone messages
            return;
        }

        try {
            JsonNode payload = getPayload(jsonDeserializer.deserializeToJsonNode(message));
            parsePayload(out, tablePath, payload);
        } catch (Exception e) {
            if (errorHandleWay == DeserializationErrorHandleWay.FAIL) {
                throw new DeserializationException(
                        JsonErrorCode.DEBEZIUM_DESERIALIZE_ERROR, e.getMessage());
            }
        }
    }

    public void parsePayload(Collector<SeaTunnelRow> out, JsonNode payload) throws IOException {
        parsePayload(out, tablePath, payload);
    }

    private void parsePayload(Collector<SeaTunnelRow> out, TablePath tablePath, JsonNode payload)
            throws IOException {
        String op = payload.get(OP_KEY).asText();

        switch (op) {
            case OP_CREATE:
            case OP_READ:
                SeaTunnelRow insert = debeziumRowConverter.parse(payload.get(DATA_AFTER));
                insert.setRowKind(RowKind.INSERT);
                if (tablePath != null) {
                    insert.setTableId(tablePath.toString());
                }
                out.collect(insert);
                break;
            case OP_UPDATE:
                SeaTunnelRow before = debeziumRowConverter.parse(payload.get(DATA_BEFORE));
                if (before == null) {
                    throw new IllegalStateException(
                            String.format(REPLICA_IDENTITY_EXCEPTION, "UPDATE"));
                }
                before.setRowKind(RowKind.UPDATE_BEFORE);
                if (tablePath != null) {
                    before.setTableId(tablePath.toString());
                }
                out.collect(before);

                SeaTunnelRow after = debeziumRowConverter.parse(payload.get(DATA_AFTER));
                after.setRowKind(RowKind.UPDATE_AFTER);

                if (tablePath != null) {
                    after.setTableId(tablePath.toString());
                }
                out.collect(after);
                break;
            case OP_DELETE:
                SeaTunnelRow delete = debeziumRowConverter.parse(payload.get(DATA_BEFORE));
                if (delete == null) {
                    throw new IllegalStateException(
                            String.format(REPLICA_IDENTITY_EXCEPTION, "DELETE"));
                }
                delete.setRowKind(RowKind.DELETE);
                if (tablePath != null) {
                    delete.setTableId(tablePath.toString());
                }
                out.collect(delete);
                break;
            default:
                throw new IllegalStateException(format("Unknown operation type '%s'.", op));
        }
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getProducedType() {
        return this.rowType;
    }

    private JsonNode getPayload(JsonNode jsonNode) {
        if (debeziumEnabledSchema) {
            return jsonNode.get(DATA_PAYLOAD);
        }
        return jsonNode;
    }
}
