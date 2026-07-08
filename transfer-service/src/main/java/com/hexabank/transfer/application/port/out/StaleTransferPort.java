package com.hexabank.transfer.application.port.out;

import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Puerto de salida: localiza transferencias atascadas en alguno de los estados no terminales dados,
 * creadas antes de un instante de corte.
 */
public interface StaleTransferPort {

    List<Transfer> findStale(Set<TransferStatus> statuses, Instant olderThan);
}
