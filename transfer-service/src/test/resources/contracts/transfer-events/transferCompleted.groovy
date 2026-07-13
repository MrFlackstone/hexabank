import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "transfer-service publica TransferCompleted en transfer-events al completar la saga"
    label "transferCompleted"
    input {
        triggeredBy("triggerTransferCompleted()")
    }
    outputMessage {
        sentTo "transfer-events"
        body([
                eventId   : anyUuid(),
                transferId: anyUuid()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.TransferCompleted")
        }
    }
}
