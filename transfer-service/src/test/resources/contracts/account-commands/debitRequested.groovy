import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "transfer-service publica DebitRequested en account-commands al iniciar la saga"
    label "debitRequested"
    input {
        triggeredBy("triggerDebitRequested()")
    }
    outputMessage {
        sentTo "account-commands"
        body([
                eventId   : anyUuid(),
                transferId: anyUuid(),
                accountId : anyUuid(),
                amount    : anyNumber()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.DebitRequested")
        }
    }
}
