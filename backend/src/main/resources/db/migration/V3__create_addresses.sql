-- Adresses de livraison des utilisateurs.
-- is_default (et non "default") car DEFAULT est un mot réservé du SQL.
CREATE TABLE addresses (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    label      VARCHAR(50)  NOT NULL,
    city       VARCHAR(100) NOT NULL,
    district   VARCHAR(100) NOT NULL,
    details    VARCHAR(255) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    -- Si un utilisateur est supprimé, ses adresses le sont aussi.
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);

-- Index unique PARTIEL : au plus une adresse par défaut par utilisateur.
-- Il ne concerne que les lignes où is_default est vrai.
CREATE UNIQUE INDEX uk_addresses_one_default_per_user ON addresses (user_id) WHERE is_default;