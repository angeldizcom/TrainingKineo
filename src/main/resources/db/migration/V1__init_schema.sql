-- Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Assessments Table
CREATE TABLE user_assessments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    primary_goal VARCHAR(255),
    fitness_level VARCHAR(255),
    training_frequency INTEGER,
    preferred_duration INTEGER,
    age INTEGER,
    weight DOUBLE PRECISION,
    height DOUBLE PRECISION,
    gender VARCHAR(50),
    injuries TEXT,
    equipment_access VARCHAR(255),
    taken_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_assessment FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Exercises Table
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(255),
    muscle_group VARCHAR(255),
    equipment VARCHAR(255),
    image_url VARCHAR(500),
    video_url VARCHAR(500)
);

-- Training Plans Table
CREATE TABLE training_plans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    goal VARCHAR(255),
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_user_plan FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Training Sessions Table
CREATE TABLE training_sessions (
    id BIGSERIAL PRIMARY KEY,
    training_plan_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    scheduled_date DATE,
    notes TEXT,
    completed_at TIMESTAMP,
    CONSTRAINT fk_plan_session FOREIGN KEY (training_plan_id) REFERENCES training_plans(id) ON DELETE CASCADE
);

-- Session Exercises Table
CREATE TABLE session_exercises (
    id BIGSERIAL PRIMARY KEY,
    training_session_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    order_index INTEGER,
    notes TEXT,
    CONSTRAINT fk_session_exercise_session FOREIGN KEY (training_session_id) REFERENCES training_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_exercise_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
);

-- Exercise Sets Table
CREATE TABLE exercise_sets (
    id BIGSERIAL PRIMARY KEY,
    session_exercise_id BIGINT NOT NULL,
    set_number INTEGER NOT NULL,
    target_reps INTEGER,
    actual_reps INTEGER,
    weight DOUBLE PRECISION,
    rpe INTEGER,
    completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_set_session_exercise FOREIGN KEY (session_exercise_id) REFERENCES session_exercises(id) ON DELETE CASCADE
);

-- Training Advice Table (from your initial request)
CREATE TABLE training_advice (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    advice TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
