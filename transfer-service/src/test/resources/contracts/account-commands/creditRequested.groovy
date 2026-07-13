import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "transfer-service publica CreditRequested en account-commands tras el débito"
    label "creditRequested"
    input {
        triggeredBy("triggerCreditRequested()")
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
            header("__TypeId__", "com.hexabank.shared.events.CreditRequested")
        }
    }
}
