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
                transferId: anyUuid(),
                accountId : anyUuid(),
                amount    : anyNumber(),
                reason    : "Fondos insuficientes"
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.DebitFailed")
        }
    }
}
