/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.format.protobuf;

import org.apache.seatunnel.api.serialization.DeserializationErrorHandleWay;
import org.apache.seatunnel.api.serialization.DeserializationException;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.TablePath;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Optional;

import static org.apache.seatunnel.format.protobuf.exception.ProtobufFormatErrorCode.DESERIALIZATION_FAILED;

public class ProtobufDeserializationSchema implements DeserializationSchema<SeaTunnelRow> {
    private static final long serialVersionUID = -7907358485475741366L;

    private final SeaTunnelRowType rowType;
    private final ProtobufToRowConverter converter;
    private final CatalogTable catalogTable;
    private final DeserializationErrorHandleWay errorHandleWay;
    private final String protoContent;
    private final String messageName;

    public ProtobufDeserializationSchema(
            CatalogTable catalogTable, DeserializationErrorHandleWay errorHandleWay) {
        this.catalogTable = catalogTable;
        this.rowType = catalogTable.getSeaTunnelRowType();
        this.messageName = catalogTable.getOptions().get("protobuf_message_name");
        this.protoContent = catalogTable.getOptions().get("protobuf_schema");
        this.errorHandleWay = errorHandleWay;
        this.converter = new ProtobufToRowConverter(protoContent, messageName, errorHandleWay);
    }

    @Override
    public SeaTunnelRow deserialize(byte[] message) throws DeserializationException {
        Descriptors.Descriptor descriptor = this.converter.getDescriptor();
        DynamicMessage dynamicMessage = null;
        try {
            dynamicMessage = DynamicMessage.parseFrom(descriptor, message);
        } catch (InvalidProtocolBufferException e) {
            throw new DeserializationException(DESERIALIZATION_FAILED, e.getMessage());
        }
        SeaTunnelRow seaTunnelRow = this.converter.converter(descriptor, dynamicMessage, rowType);
        Optional<TablePath> tablePath =
                Optional.ofNullable(catalogTable).map(CatalogTable::getTablePath);
        if (tablePath.isPresent()) {
            seaTunnelRow.setTableId(tablePath.toString());
        }
        return seaTunnelRow;
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getProducedType() {
        return this.rowType;
    }
}
