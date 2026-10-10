-- Esquema de datos del Banco XYZ (EFT) - fin_legacy_data.
-- Se usa CREATE TABLE IF NOT EXISTS para que sea idempotente.

CREATE TABLE IF NOT EXISTS cuenta (
    cuenta_id BIGINT NOT NULL,
    nombre     VARCHAR(255),
    saldo      DECIMAL(19, 2),
    edad       INTEGER,
    tipo       VARCHAR(255),
    PRIMARY KEY (cuenta_id)
);

CREATE TABLE IF NOT EXISTS transaccion (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    cuenta_id   BIGINT,
    fecha       DATE,
    tipo        VARCHAR(255),
    monto       DECIMAL(19, 2),
    descripcion VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS movimiento (
    id    BIGINT NOT NULL,
    fecha DATE,
    monto DECIMAL(19, 2),
    tipo  VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS anomalia (
    id     BIGINT NOT NULL AUTO_INCREMENT,
    fecha  DATE,
    monto  DECIMAL(19, 2),
    tipo   VARCHAR(255),
    motivo VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS resumen_transacciones (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    fecha              DATE,
    total_transacciones INTEGER,
    total_anomalias     INTEGER,
    monto_total         DECIMAL(19, 2),
    PRIMARY KEY (id)
);
