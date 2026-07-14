import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "account-service publica MoneyDebited en account-events al debitar con fondos suficientes"
    label "moneyDebited"
    input {
        triggeredBy("triggerMoneyDebited()")
    }
    outputMessage {
        sentTo "account-events"
        body([
                eventId   : anyUuid(),
                transferId: $(consumer("10000000-0000-0000-0000-000000000001"), producer(regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))),
                accountId : anyUuid(),
                amount    : anyNumber()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.MoneyDebited")
        }
    }
}
