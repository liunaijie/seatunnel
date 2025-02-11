package org.apache.seatunnel.connectors.seatunnel.kafka.exception;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;

public class KafkaSerializationException extends KafkaConnectorException {

    public KafkaSerializationException(SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage) {
        super(seaTunnelErrorCode, errorMessage);
    }
}
