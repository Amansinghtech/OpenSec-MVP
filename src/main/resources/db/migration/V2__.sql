ALTER TABLE users
    ADD created_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE users
    ADD enabled BOOLEAN;

ALTER TABLE users
    ADD full_name VARCHAR(255);

ALTER TABLE users
    ADD password VARCHAR(255);

ALTER TABLE users
    ADD phone VARCHAR(15);

ALTER TABLE users
    ADD updated_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE users
    ADD username VARCHAR(255);

ALTER TABLE users
    ALTER COLUMN enabled SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN password SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN phone SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

CREATE INDEX idx_user_email ON users (email);

CREATE INDEX idx_user_phone ON users (phone);

CREATE INDEX idx_user_username ON users (username);

ALTER TABLE users
DROP
COLUMN name;

ALTER TABLE users
DROP
COLUMN id;

ALTER TABLE users
    ADD id UUID NOT NULL PRIMARY KEY;