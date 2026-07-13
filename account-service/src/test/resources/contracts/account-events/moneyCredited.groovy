import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "account-service publica MoneyCredited en account-events al acreditar"
    label "moneyCredited"
    input {
        triggeredBy("triggerMoneyCredited()")
    }
    outputMessage {
        sentTo "account-events"
        body([
                eventId   : anyUuid(),
                transferId: anyUuid(),
                accountId : anyUuid(),
                amount    : anyNumber()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.MoneyCredited")
        }
    }
}
