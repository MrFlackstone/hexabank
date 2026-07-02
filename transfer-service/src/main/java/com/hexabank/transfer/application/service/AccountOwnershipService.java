package com.hexabank.transfer.application.service;

import com.hexabank.transfer.application.port.in.AccountProjectionUseCase;
import com.hexabank.transfer.application.port.out.AccountOwnershipPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Mantiene la proyección de propietarios de cuenta. Idempotente por partida doble: la guarda de
 * {@code processed_events} descarta redeliveries y la PK de {@code account_ownership} hace del
 * registro un upsert natural.
 */
public class AccountOwnershipService implements AccountProjectionUseCase {

    private final AccountOwnershipPort accountOwnership;
    private final ProcessedEventPort processedEvents;

    public AccountOwnershipService(AccountOwnershipPort accountOwnership, ProcessedEventPort processedEvents) {
        this.accountOwnership = accountOwnership;
        this.processedEvents = processedEvents;
    }

    @Override
    @Transactional
    public void onAccountOpened(UUID eventId, UUID accountId, String ownerId) {
        if (processedEvents.alreadyProcessed(eventId)) {
            return;
        }
        accountOwnership.register(accountId, ownerId);
        processedEvents.markProcessed(eventId);
    }
}
