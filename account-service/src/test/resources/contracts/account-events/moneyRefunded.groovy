import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "account-service publica MoneyRefunded en account-events al reembolsar (compensación)"
    label "moneyRefunded"
    input {
        triggeredBy("triggerMoneyRefunded()")
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
            header("__TypeId__", "com.hexabank.shared.events.MoneyRefunded")
        }
    }
}
