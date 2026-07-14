import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "account-service publica AccountOpened en account-events al crear una cuenta"
    label "accountOpened"
    input {
        triggeredBy("triggerAccountOpened()")
    }
    outputMessage {
        sentTo "account-events"
        body([
                eventId  : anyUuid(),
                accountId: $(consumer("20000000-0000-0000-0000-000000000009"), producer(regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))),
                ownerId  : $(consumer("alice"), producer(anyNonBlankString()))
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.AccountOpened")
        }
    }
}
