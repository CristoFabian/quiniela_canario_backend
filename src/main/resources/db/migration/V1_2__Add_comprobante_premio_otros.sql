-- Agrega la columna comprobante_premio_otros_url a la tabla ganador_quiniela
-- Esta migración sirve para bases MySQL/MariaDB. Ajusta si usas otro motor.

ALTER TABLE ganador_quiniela
  ADD COLUMN comprobante_premio_otros_url VARCHAR(255) NULL;

-- Nota: Si usas H2 en tests, Flyway aplicará la misma sentencia en H2.
