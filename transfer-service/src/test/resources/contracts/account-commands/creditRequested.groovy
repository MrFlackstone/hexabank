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
                accountId : $(consumer("40000000-0000-0000-0000-000000000002"), producer(regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))),
                amount    : $(consumer(40.00), producer(anyNumber()))
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.CreditRequested")
        }
    }
}
