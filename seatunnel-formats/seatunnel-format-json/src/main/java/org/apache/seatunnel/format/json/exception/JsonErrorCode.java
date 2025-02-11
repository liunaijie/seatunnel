package org.apache.seatunnel.format.json.exception;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;

public enum JsonErrorCode implements SeaTunnelErrorCode {
    CANAL_DESERIALIZE_ERROR("10001", "Canal deserialization error"),
    DEBEZIUM_DESERIALIZE_ERROR("10001", "Canal deserialization error"),
    ;

    private final String code;
    private final String description;

    JsonErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
