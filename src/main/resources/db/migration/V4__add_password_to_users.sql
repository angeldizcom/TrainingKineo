-- ============================================================
-- V4 — Añade password_hash a la tabla users
-- ============================================================
-- El campo es nullable en esta migración para no romper
-- los registros existentes del seed (V2).
-- En producción debería ser NOT NULL desde el primer día.
-- ============================================================

ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);

-- El usuario de test del seed no tiene contraseña todavía.
-- Para asignarle una desde psql:
--   UPDATE users SET password_hash = '<bcrypt_hash>' WHERE id = 1;
-- Genera el hash con: https://bcrypt-generator.com (rounds: 10)
