package org.apache.seatunnel.connectors.seatunnel.kafka.exception;

import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;

public class KafkaDeserializationException extends KafkaConnectorException {

    public KafkaDeserializationException(
            SeaTunnelErrorCode seaTunnelErrorCode, String errorMessage) {
        super(seaTunnelErrorCode, errorMessage);
    }
}
