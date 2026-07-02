package com.hexabank.transfer.application.service;

import com.hexabank.transfer.application.port.out.AccountOwnershipPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitarios de {@link AccountOwnershipService}: proyección idempotente de AccountOpened. */
class AccountOwnershipServiceTest {

    private AccountOwnershipPort accountOwnership;
    private ProcessedEventPort processedEvents;
    private AccountOwnershipService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        accountOwnership = mock(AccountOwnershipPort.class);
        processedEvents = mock(ProcessedEventPort.class);
        service = new AccountOwnershipService(accountOwnership, processedEvents);
    }

    @Test
    @DisplayName("registra el propietario y marca el evento como procesado")
    void registersOwnership() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);

        service.onAccountOpened(eventId, accountId, "alice-sub");

        verify(accountOwnership).register(accountId, "alice-sub");
        verify(processedEvents).markProcessed(eventId);
    }

    @Test
    @DisplayName("evento ya procesado: idempotente, no vuelve a registrar")
    void redeliveryIsNoOp() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(true);

        service.onAccountOpened(eventId, accountId, "alice-sub");

        verify(accountOwnership, never()).register(accountId, "alice-sub");
        verify(processedEvents, never()).markProcessed(eventId);
    }
}
