import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "transfer-service publica TransferFailed en transfer-events al fracasar la saga"
    label "transferFailed"
    input {
        triggeredBy("triggerTransferFailed()")
    }
    outputMessage {
        sentTo "transfer-events"
        body([
                eventId   : anyUuid(),
                transferId: $(consumer("50000000-0000-0000-0000-000000000002"), producer(regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))),
                reason    : anyNonBlankString()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.TransferFailed")
        }
    }
}
