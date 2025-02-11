package org.apache.seatunnel.api.serialization;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelRuntimeException;

public class DeserializationException extends SeaTunnelRuntimeException {

    public DeserializationException(SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage) {
        super(seaTunnelErrorCode, errorMessage);
    }
}
