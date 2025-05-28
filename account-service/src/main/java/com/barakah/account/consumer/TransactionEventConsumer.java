package com.barakah.account.consumer;

import com.barakah.account.service.BalanceUpdateService;
import com.barakah.shared.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final BalanceUpdateService balanceUpdateService;

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-events:transaction-events}",
            groupId = "${spring.kafka.consumer.group-id:account-service}",
            containerFactory = "transactionEventKafkaListenerContainerFactory"
    )
    public void handleTransactionEvent(@Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received transaction event: {} from topic: {}, partition: {}, offset: {}",
                event.getEventType(), topic, partition, offset);

        try {

            if (event.getEventType() == TransactionEvent.EventType.BALANCE_UPDATE_REQUIRED
                    || event.getEventType() == TransactionEvent.EventType.TRANSACTION_CREATED
                    || event.getEventType() == TransactionEvent.EventType.TRANSACTION_STATUS_CHANGED) {

                if ("COMPLETED".equals(event.getStatus())
                        || event.getEventType() == TransactionEvent.EventType.BALANCE_UPDATE_REQUIRED) {

                    balanceUpdateService.updateBalanceFromTransaction(event);

                    log.info("Successfully processed transaction event: {} for transaction: {}",
                            event.getEventType(), event.getTransactionId());
                } else {
                    log.debug("Skipping balance update for transaction {} with status: {}",
                            event.getTransactionId(), event.getStatus());
                }
            } else {
                log.debug("Ignoring transaction event type: {} for transaction: {}",
                        event.getEventType(), event.getTransactionId());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing transaction event: {} for transaction: {}, error: {}",
                    event.getEventType(), event.getTransactionId(), e.getMessage(), e);

            throw e;
        }
    }
}
