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
                accountId: anyUuid(),
                ownerId  : anyNonBlankString()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.AccountOpened")
        }
    }
}
