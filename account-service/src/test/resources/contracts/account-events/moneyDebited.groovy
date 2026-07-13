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
                transferId: anyUuid(),
                accountId : anyUuid(),
                amount    : anyNumber()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.MoneyDebited")
        }
    }
}
