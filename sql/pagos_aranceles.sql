-- =====================================================================
--  Sistema de Gestión de Pagos y Aranceles - IPESMI
--  Programación III - Parcial 2 - Actividad 7
--  Motor: MySQL 8 / 9
-- =====================================================================

DROP DATABASE IF EXISTS pagos_aranceles;
CREATE DATABASE pagos_aranceles CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pagos_aranceles;

-- ---------------------------------------------------------------------
--  CREATE TABLE
-- ---------------------------------------------------------------------

CREATE TABLE estudiante (
    legajo          INT           NOT NULL,
    nombre          VARCHAR(60)   NOT NULL,
    apellido        VARCHAR(60)   NOT NULL,
    email           VARCHAR(100)  NOT NULL,
    porcentaje_beca DOUBLE        NOT NULL DEFAULT 0,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_estudiante PRIMARY KEY (legajo),
    CONSTRAINT uq_estudiante_email UNIQUE (email),
    CONSTRAINT ck_estudiante_beca CHECK (porcentaje_beca BETWEEN 0 AND 100)
);

CREATE TABLE concepto (
    codigo       VARCHAR(20)    NOT NULL,
    descripcion  VARCHAR(120)   NOT NULL,
    monto        DECIMAL(12,2)  NOT NULL,
    tipo         VARCHAR(20)    NOT NULL,   -- MATRICULA | CUOTA | EXAMEN | OTRO
    becable      BOOLEAN        NOT NULL DEFAULT TRUE,
    vencimiento  DATE           NULL,
    CONSTRAINT pk_concepto PRIMARY KEY (codigo),
    CONSTRAINT ck_concepto_monto CHECK (monto > 0)
);

-- Herencia JPA SINGLE_TABLE: la columna "medio" es el discriminador
-- (EFECTIVO | TARJETA | TRANSFERENCIA).
CREATE TABLE pago (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    medio             VARCHAR(20)    NOT NULL,
    legajo_estudiante INT            NOT NULL,
    codigo_concepto   VARCHAR(20)    NOT NULL,
    fecha             DATE           NOT NULL,
    monto             DECIMAL(12,2)  NOT NULL,
    estado            VARCHAR(20)    NOT NULL,   -- PENDIENTE | ACREDITADO | ANULADO
    referencia        VARCHAR(60)    NULL,       -- recibo / últimos 4 dígitos / nro. de operación
    cuotas            INT            NULL,       -- solo TARJETA
    banco             VARCHAR(60)    NULL,       -- solo TRANSFERENCIA
    CONSTRAINT pk_pago PRIMARY KEY (id),
    CONSTRAINT fk_pago_estudiante FOREIGN KEY (legajo_estudiante) REFERENCES estudiante (legajo),
    CONSTRAINT fk_pago_concepto   FOREIGN KEY (codigo_concepto)   REFERENCES concepto (codigo),
    CONSTRAINT ck_pago_monto CHECK (monto > 0)
);

CREATE INDEX idx_pago_estudiante ON pago (legajo_estudiante);
CREATE INDEX idx_pago_concepto   ON pago (codigo_concepto);

-- ---------------------------------------------------------------------
--  INSERT INTO (datos de ejemplo)
-- ---------------------------------------------------------------------

INSERT INTO estudiante (legajo, nombre, apellido, email, porcentaje_beca, activo) VALUES
    (1001, 'Lucía',    'Benítez',   'lucia.benitez@ipesmi.edu.ar',   0,   TRUE),
    (1002, 'Matías',   'Fernández', 'matias.fernandez@ipesmi.edu.ar', 50,  TRUE),
    (1003, 'Camila',   'Duarte',    'camila.duarte@ipesmi.edu.ar',   0,   TRUE),
    (1004, 'Joaquín',  'Ramírez',   'joaquin.ramirez@ipesmi.edu.ar', 25,  TRUE),
    (1005, 'Valentina','Sosa',      'valentina.sosa@ipesmi.edu.ar',  100, TRUE),
    (1006, 'Tomás',    'Acosta',    'tomas.acosta@ipesmi.edu.ar',    0,   FALSE);

INSERT INTO concepto (codigo, descripcion, monto, tipo, becable, vencimiento) VALUES
    ('MAT-2026', 'Matrícula anual 2026',  45000.00, 'MATRICULA', FALSE, '2026-03-01'),
    ('CUO-03',   'Cuota marzo',           32000.00, 'CUOTA',     TRUE,  '2026-03-10'),
    ('CUO-04',   'Cuota abril',           32000.00, 'CUOTA',     TRUE,  '2026-04-10'),
    ('CUO-05',   'Cuota mayo',            32000.00, 'CUOTA',     TRUE,  '2026-05-10'),
    ('EXA-JUL',  'Derecho de examen julio', 8500.00, 'EXAMEN',   FALSE, '2026-07-01');

INSERT INTO pago (medio, legajo_estudiante, codigo_concepto, fecha, monto, estado, referencia, cuotas, banco) VALUES
    ('EFECTIVO',      1001, 'MAT-2026', '2026-02-20', 45000.00, 'ACREDITADO', 'REC-0001', NULL, NULL),
    ('TARJETA',       1001, 'CUO-03',   '2026-03-08', 32000.00, 'ACREDITADO', '4821',     3,    NULL),
    ('TRANSFERENCIA', 1001, 'CUO-04',   '2026-04-09', 32000.00, 'PENDIENTE',  'OP-778812', NULL, 'Banco Macro'),
    ('EFECTIVO',      1002, 'MAT-2026', '2026-02-25', 45000.00, 'ACREDITADO', 'REC-0002', NULL, NULL),
    ('TRANSFERENCIA', 1002, 'CUO-03',   '2026-03-10', 16000.00, 'ACREDITADO', 'OP-552101', NULL, 'Banco Nación'),
    ('TARJETA',       1003, 'MAT-2026', '2026-02-28', 45000.00, 'ACREDITADO', '0937',     1,    NULL),
    ('EFECTIVO',      1003, 'CUO-03',   '2026-03-12', 20000.00, 'ACREDITADO', 'REC-0003', NULL, NULL),
    ('TRANSFERENCIA', 1004, 'MAT-2026', '2026-03-01', 45000.00, 'ACREDITADO', 'OP-610045', NULL, 'Banco Galicia'),
    ('TARJETA',       1004, 'CUO-03',   '2026-03-09', 24000.00, 'ANULADO',    '1150',     1,    NULL),
    ('EFECTIVO',      1005, 'MAT-2026', '2026-02-18', 45000.00, 'ACREDITADO', 'REC-0004', NULL, NULL);

-- ---------------------------------------------------------------------
--  UPDATE
-- ---------------------------------------------------------------------

-- Se acredita la transferencia pendiente de Lucía Benítez
UPDATE pago SET estado = 'ACREDITADO' WHERE referencia = 'OP-778812';

-- Se aplica una beca del 30 % a Camila Duarte
UPDATE estudiante SET porcentaje_beca = 30 WHERE legajo = 1003;

-- ---------------------------------------------------------------------
--  SELECT (consultas de control)
-- ---------------------------------------------------------------------

-- 1) Pagos con estudiante y concepto
SELECT p.id, p.fecha, CONCAT(e.apellido, ', ', e.nombre) AS estudiante,
       c.descripcion AS concepto, p.medio, p.monto, p.estado
FROM pago p
JOIN estudiante e ON e.legajo = p.legajo_estudiante
JOIN concepto   c ON c.codigo = p.codigo_concepto
ORDER BY p.fecha;

-- 2) Distribución de lo recaudado por medio de pago (datos del PieChart)
SELECT medio, COUNT(*) AS cantidad, SUM(monto) AS total
FROM pago
WHERE estado = 'ACREDITADO'
GROUP BY medio;

-- 3) Saldo pendiente por estudiante activo:
--    importe de cada concepto (con beca si es becable) - lo acreditado
SELECT e.legajo, CONCAT(e.apellido, ', ', e.nombre) AS estudiante, e.porcentaje_beca,
       ROUND(SUM(GREATEST(
           CASE WHEN c.becable THEN c.monto * (1 - e.porcentaje_beca / 100) ELSE c.monto END
           - COALESCE((SELECT SUM(p.monto) FROM pago p
                       WHERE p.legajo_estudiante = e.legajo
                         AND p.codigo_concepto   = c.codigo
                         AND p.estado = 'ACREDITADO'), 0),
           0)), 2) AS saldo_pendiente
FROM estudiante e
CROSS JOIN concepto c
WHERE e.activo = TRUE
GROUP BY e.legajo, e.apellido, e.nombre, e.porcentaje_beca
ORDER BY saldo_pendiente DESC;
