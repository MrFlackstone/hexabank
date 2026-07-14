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
                transferId: $(consumer("10000000-0000-0000-0000-000000000003"), producer(regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))),
                accountId : anyUuid(),
                amount    : $(consumer(40.00), producer(anyNumber()))
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.MoneyCredited")
        }
    }
}
