package com.hexabank.account.domain;

import com.hexabank.account.domain.exception.InsufficientFundsException;

import java.util.Objects;

/**
 * Agregado raíz del dominio: una cuenta bancaria con su saldo.
 *
 * <p>Concentra las <strong>reglas de negocio</strong> (invariantes de saldo) y no depende de ningún
 * framework: ni Spring, ni JPA, ni Kafka.</p>
 *
 * <p>El campo {@code version} es el contador de <strong>bloqueo optimista</strong>: el dominio lo
 * modela como un {@code long} y el adaptador JPA lo marca con {@code @Version}.</p>
 */
public class Account {

    private final AccountId id;
    private final String holder;
    /** Propietario ({@code sub} del creador); nulo en cuentas previas a la columna {@code owner_id}. */
    private final OwnerId ownerId;
    private Money balance;
    private long version;

    /** Constructor de reconstrucción (lo usa el adaptador de persistencia al cargar de la BD). */
    public Account(AccountId id, String holder, OwnerId ownerId, Money balance, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.holder = requireHolder(holder);
        this.ownerId = ownerId;
        this.balance = Objects.requireNonNull(balance, "balance");
        this.version = version;
    }

    /** Factoría de creación de una cuenta nueva con un saldo inicial y su propietario. */
    public static Account open(String holder, Money initialBalance, OwnerId ownerId) {
        return new Account(AccountId.newId(), holder, Objects.requireNonNull(ownerId, "ownerId"),
                initialBalance, 0L);
    }

    /**
     * Indica si la cuenta pertenece al sujeto dado. Una cuenta sin propietario (dato anterior a la
     * incorporación de {@code owner_id}) no pertenece a nadie: solo STAFF puede operarla.
     */
    public boolean isOwnedBy(String subject) {
        return ownerId != null && ownerId.value().equals(subject);
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

    /** Propietario de la cuenta; nulo en cuentas anteriores a la columna {@code owner_id}. */
    public OwnerId ownerId() {
        return ownerId;
    }

    public Money balance() {
        return balance;
    }

    public long version() {
        return version;
    }
}
