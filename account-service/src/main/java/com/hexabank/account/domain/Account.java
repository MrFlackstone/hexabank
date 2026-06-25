package com.hexabank.account.domain;

import com.hexabank.account.domain.exception.InsufficientFundsException;

import java.util.Objects;

/**
 * Agregado raíz del dominio: una cuenta bancaria con su saldo.
 *
 * <p>Concentra las <strong>reglas de negocio</strong> (invariantes de saldo) y no depende de ningún
 * framework: ni Spring, ni JPA, ni Kafka. Esa es la regla central de la arquitectura hexagonal —
 * el dominio es el núcleo y todo lo demás (infraestructura) depende de él, nunca al revés.</p>
 *
 * <h2>Concurrencia (gap declarado para la entrevista)</h2>
 * <p>El campo {@code version} es el contador de <strong>bloqueo optimista</strong>. El dominio lo
 * modela como un simple {@code long}; es el adaptador JPA quien lo marca con {@code @Version} para
 * que Hibernate detecte escrituras concurrentes (dos operaciones sobre la misma cuenta) y rechace
 * la que llega con una versión obsoleta. Así se evita un sobregiro por condición de carrera sin
 * recurrir a bloqueo pesimista (no se bloquea la fila).</p>
 */
public class Account {

    private final AccountId id;
    private final String holder;
    private Money balance;
    private long version;

    /** Constructor de reconstrucción (lo usa el adaptador de persistencia al cargar de la BD). */
    public Account(AccountId id, String holder, Money balance, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.holder = requireHolder(holder);
        this.balance = Objects.requireNonNull(balance, "balance");
        this.version = version;
    }

    /** Factoría de creación de una cuenta nueva con un saldo inicial. */
    public static Account open(String holder, Money initialBalance) {
        return new Account(AccountId.newId(), holder, initialBalance, 0L);
    }

    /**
     * Debita un importe del saldo aplicando la invariante de fondos suficientes.
     *
     * @throws InsufficientFundsException si el saldo es menor que el importe a debitar.
     */
    public void debit(Money amount) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(id);
        }
        this.balance = balance.subtract(amount);
    }

    /** Acredita (ingresa) un importe en el saldo. */
    public void credit(Money amount) {
        this.balance = balance.add(amount);
    }

    private static String requireHolder(String holder) {
        if (holder == null || holder.isBlank()) {
            throw new IllegalArgumentException("El titular de la cuenta es obligatorio");
        }
        return holder;
    }

    public AccountId id() {
        return id;
    }

    public String holder() {
        return holder;
    }

    public Money balance() {
        return balance;
    }

    public long version() {
        return version;
    }
}
