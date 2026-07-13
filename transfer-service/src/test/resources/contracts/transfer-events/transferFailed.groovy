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
                transferId: anyUuid(),
                reason    : anyNonBlankString()
        ])
        headers {
            header("__TypeId__", "com.hexabank.shared.events.TransferFailed")
        }
    }
}
