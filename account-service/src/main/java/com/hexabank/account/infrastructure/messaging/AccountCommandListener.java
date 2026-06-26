package com.hexabank.account.infrastructure.messaging;

import com.hexabank.account.application.port.in.AccountCommand;
import com.hexabank.account.application.port.in.CreditAccountUseCase;
import com.hexabank.account.application.port.in.DebitAccountUseCase;
import com.hexabank.account.application.port.in.RefundAccountUseCase;
import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.RefundRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada que consume los comandos de {@code account-commands} y los delega a los casos
 * de uso, traduciendo el evento de transporte al comando de aplicación {@link AccountCommand}.
 */
@Component
public class AccountCommandListener {

    private static final Logger log = LoggerFactory.getLogger(AccountCommandListener.class);

    private final DebitAccountUseCase debitAccountUseCase;
    private final CreditAccountUseCase creditAccountUseCase;
    private final RefundAccountUseCase refundAccountUseCase;

    public AccountCommandListener(DebitAccountUseCase debitAccountUseCase,
                                  CreditAccountUseCase creditAccountUseCase,
                                  RefundAccountUseCase refundAccountUseCase) {
        this.debitAccountUseCase = debitAccountUseCase;
        this.creditAccountUseCase = creditAccountUseCase;
        this.refundAccountUseCase = refundAccountUseCase;
    }

    @KafkaListener(topics = KafkaTopics.ACCOUNT_COMMANDS, containerFactory = "kafkaListenerContainerFactory")
    public void onCommand(DomainEvent event) {
        switch (event) {
            case DebitRequested c -> debitAccountUseCase.debit(toCommand(c.eventId(), c.transferId(), c.accountId(), c.amount()));
            case CreditRequested c -> creditAccountUseCase.credit(toCommand(c.eventId(), c.transferId(), c.accountId(), c.amount()));
            case RefundRequested c -> refundAccountUseCase.refund(toCommand(c.eventId(), c.transferId(), c.accountId(), c.amount()));
            default -> log.warn("Evento ignorado en account-commands (no es un comando): {}",
                    event.getClass().getSimpleName());
        }
    }

    private static AccountCommand toCommand(java.util.UUID eventId, java.util.UUID transferId,
                                            java.util.UUID accountId, java.math.BigDecimal amount) {
        return new AccountCommand(eventId, transferId, accountId, amount);
    }
}
