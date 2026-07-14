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
                transferId: $(consumer("50000000-0000-0000-0000-000000000001"), producer(regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")))
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.TransferCompleted")
        }
    }
}
