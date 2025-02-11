package org.apache.seatunnel.api.serialization;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;
import org.apache.seatunnel.common.exception.SeaTunnelRuntimeException;

public class SerializationException extends SeaTunnelRuntimeException {

    public SerializationException(SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage) {
        super(seaTunnelErrorCode, errorMessage);
    }
}
