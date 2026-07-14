import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "account-service publica DebitFailed en account-events al debitar sin fondos suficientes"
    label "debitFailed"
    input {
        triggeredBy("triggerDebitFailed()")
    }
    outputMessage {
        sentTo "account-events"
        body([
                eventId   : anyUuid(),
                transferId: $(consumer("10000000-0000-0000-0000-000000000002"), producer(regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))),
                accountId : anyUuid(),
                amount    : anyNumber(),
                reason    : "Fondos insuficientes"
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.DebitFailed")
        }
    }
}
