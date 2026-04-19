-- SEED DE EJERCICIOS PARA EL CATÁLOGO KINEO
INSERT INTO exercises (name, description, category, muscle_group, equipment) VALUES
-- PECHO (CHEST)
('Press de Banca con Barra', 'Ejercicio básico de empuje horizontal para fuerza y masa.', 'STRENGTH', 'CHEST', 'BARBELL'),
('Press inclinado con Mancuernas', 'Enfoque en la parte superior del pectoral.', 'STRENGTH', 'CHEST', 'DUMBBELL'),
('Aperturas con Mancuernas', 'Aislamiento para estiramiento pectoral.', 'STRENGTH', 'CHEST', 'DUMBBELL'),
('Flexiones de Brazos', 'Básico con peso corporal.', 'STRENGTH', 'CHEST', 'BODYWEIGHT'),

-- ESPALDA (BACK)
('Dominadas Pronas', 'Tracción vertical para amplitud de espalda.', 'STRENGTH', 'BACK', 'BODYWEIGHT'),
('Remo con Barra', 'Tracción horizontal para densidad de espalda.', 'STRENGTH', 'BACK', 'BARBELL'),
('Remo con Mancuerna a una mano', 'Trabajo unilateral de dorsal.', 'STRENGTH', 'BACK', 'DUMBBELL'),
('Jalón al Pecho', 'Variante de tracción vertical en máquina.', 'STRENGTH', 'BACK', 'MACHINE'),

-- PIERNAS (LEGS)
('Sentadilla Trasera con Barra', 'El rey de los ejercicios de pierna.', 'STRENGTH', 'LEGS', 'BARBELL'),
('Prensa de Piernas', 'Empuje de piernas en máquina para hipertrofia.', 'STRENGTH', 'LEGS', 'MACHINE'),
('Peso Muerto Rumano', 'Enfoque en isquiotibiales y glúteos.', 'STRENGTH', 'LEGS', 'BARBELL'),
('Extensiones de Cuádriceps', 'Aislamiento de cuádriceps en máquina.', 'STRENGTH', 'LEGS', 'MACHINE'),
('Zancadas con Mancuernas', 'Trabajo dinámico de pierna y equilibrio.', 'STRENGTH', 'LEGS', 'DUMBBELL'),

-- HOMBROS (SHOULDERS)
('Press Militar con Barra', 'Empuje vertical para hombro global.', 'STRENGTH', 'SHOULDERS', 'BARBELL'),
('Elevaciones Laterales', 'Aislamiento del deltoides lateral.', 'STRENGTH', 'SHOULDERS', 'DUMBBELL'),
('Face Pulls', 'Salud del hombro y deltoides posterior.', 'STRENGTH', 'SHOULDERS', 'CABLE'),

-- BRAZOS (ARMS)
('Curl de Bíceps con Barra', 'Básico de flexión de codo.', 'STRENGTH', 'ARMS', 'BARBELL'),
('Press Francés', 'Extensión de tríceps para masa de brazo.', 'STRENGTH', 'ARMS', 'BARBELL'),
('Martillo con Mancuernas', 'Enfoque en braquial y antebrazo.', 'STRENGTH', 'ARMS', 'DUMBBELL'),
('Extensiones de Tríceps en Polea', 'Aislamiento de tríceps.', 'STRENGTH', 'ARMS', 'CABLE');

-- 1. Crear usuario de prueba
INSERT INTO users (username, email, joined_at)
VALUES ('KineoTester', 'test@kineo.com', CURRENT_TIMESTAMP);

-- 2. Crear Assessment básico (para que el MCP no de error)
INSERT INTO user_assessments (user_id, primary_goal, fitness_level, training_frequency, equipment_access)
VALUES (1, 'GAIN_MUSCLE', 'INTERMEDIATE', 4, 'GYM');

-- 3. Crear un Plan de Entrenamiento activo
INSERT INTO training_plans (user_id, name, goal, status, start_date)
VALUES (1, 'Build Alfa 1.0', 'HYPERTROPHY', 'ACTIVE', CURRENT_DATE);