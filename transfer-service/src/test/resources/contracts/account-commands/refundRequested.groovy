import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "transfer-service publica RefundRequested en account-commands al compensar"
    label "refundRequested"
    input {
        triggeredBy("triggerRefundRequested()")
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
            header("__TypeId__", "com.hexabank.shared.events.RefundRequested")
        }
    }
}
