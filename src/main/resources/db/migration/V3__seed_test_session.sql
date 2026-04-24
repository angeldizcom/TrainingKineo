-- ============================================================
-- V3 — Seed: Sesión de prueba para testear Workout Execution
-- ============================================================
-- Prerequisitos (creados en V2):
--   · Usuario id=1  (KineoTester)
--   · Plan id=1     (Build Alfa 1.0 - ACTIVE)
--   · Catálogo de 20 ejercicios
--
-- Resultado de esta migración:
--   · 1 TrainingSession  (id=1) — "Push Day A"
--   · 4 SessionExercise  ordenados por order_index
--   · 12 ExerciseSet     (3 series × 4 ejercicios)
--
-- Todos los campos de rendimiento (actual_reps, weight) en NULL:
-- simulan el estado real antes de que el usuario empiece a entrenar.
-- ============================================================

-- ── 1. Sesión ────────────────────────────────────────────────
INSERT INTO training_sessions (training_plan_id, name, scheduled_date, notes, completed_at)
VALUES (1, 'Push Day A', CURRENT_DATE, 'Sesión de empuje: pecho, hombros y tríceps', NULL);

-- ── 2. Ejercicios de la sesión ───────────────────────────────
-- Usamos subqueries por nombre para no asumir IDs concretos
-- (robustez frente a reordenamientos del seed de V2)

INSERT INTO session_exercises (training_session_id, exercise_id, order_index, notes)
VALUES
    (1,
     (SELECT id FROM exercises WHERE name = 'Press de Banca con Barra'),
     1,
     'Calentamiento: 1 serie ligera antes de las series de trabajo'),

    (1,
     (SELECT id FROM exercises WHERE name = 'Press inclinado con Mancuernas'),
     2,
     'Controlar la bajada en 3 segundos'),

    (1,
     (SELECT id FROM exercises WHERE name = 'Press Militar con Barra'),
     3,
     'Core apretado durante todo el movimiento'),

    (1,
     (SELECT id FROM exercises WHERE name = 'Extensiones de Tríceps en Polea'),
     4,
     'Codo fijo, solo mueve el antebrazo');

-- ── 3. Series (ExerciseSets) ─────────────────────────────────
-- Estado inicial: target_reps y rpe definidos por la IA.
-- actual_reps, weight y completed = false → pendiente de ejecutar.

-- Press de Banca con Barra (session_exercise_id resuelto por subquery)
INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 1, 8, NULL, NULL, 8, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press de Banca con Barra';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 2, 8, NULL, NULL, 8, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press de Banca con Barra';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 3, 8, NULL, NULL, 8, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press de Banca con Barra';

-- Press inclinado con Mancuernas
INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 1, 10, NULL, NULL, 7, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press inclinado con Mancuernas';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 2, 10, NULL, NULL, 7, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press inclinado con Mancuernas';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 3, 10, NULL, NULL, 7, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press inclinado con Mancuernas';

-- Press Militar con Barra
INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 1, 10, NULL, NULL, 7, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press Militar con Barra';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 2, 10, NULL, NULL, 7, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press Militar con Barra';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 3, 10, NULL, NULL, 7, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Press Militar con Barra';

-- Extensiones de Tríceps en Polea
INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 1, 12, NULL, NULL, 6, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Extensiones de Tríceps en Polea';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 2, 12, NULL, NULL, 6, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Extensiones de Tríceps en Polea';

INSERT INTO exercise_sets (session_exercise_id, set_number, target_reps, actual_reps, weight, rpe, completed, created_at)
SELECT se.id, 3, 12, NULL, NULL, 6, FALSE, CURRENT_TIMESTAMP
FROM session_exercises se
JOIN exercises e ON se.exercise_id = e.id
WHERE se.training_session_id = 1 AND e.name = 'Extensiones de Tríceps en Polea';
