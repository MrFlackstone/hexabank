package com.hexabank.transfer.application.port.out;

/**
 * Puerto de salida para las métricas de negocio de la saga. El núcleo solo conoce esta abstracción;
 * el adaptador (Micrometer/Prometheus) vive en infraestructura. Así la observabilidad entra como un
 * adaptador más, sin filtrar la dependencia de métricas a la capa de aplicación.
 */
public interface TransferMetricsPort {

    /** Una transferencia ha llegado a estado COMPLETED. */
    void transferCompleted();

    /** Una transferencia ha terminado en FAILED (débito rechazado o crédito revertido). */
    void transferFailed();
}
