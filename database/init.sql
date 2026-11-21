-- La base se crée à part (psql ne peut pas changer de base au milieu d'un script sans \c) :
--   CREATE DATABASE escapin;
--   \c escapin

-- Nécessaire pour le type GEOGRAPHY
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TYPE visibilite_type AS ENUM ('friends', 'private', 'public');
CREATE TYPE provider_type AS ENUM ('google', 'github', 'apple', 'us');

CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pseudo text NOT NULL,
    email text,
    password_hash text,
    name text,
    surname text,
    ville_origine text,
    url_photo_profil text,
    url_photo_fond_profil text,
    cgu_accepted_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX users_pseudo_lower_key ON users (lower(pseudo));
CREATE UNIQUE INDEX users_email_lower_key ON users (lower(email)) WHERE email IS NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_pseudo_format CHECK (pseudo ~ '^[A-Za-z0-9_.]{3,30}$');

CREATE TABLE auth_provider (
    id_user BIGINT NOT NULL REFERENCES users(id),
    provider provider_type NOT NULL,
    provider_uid text NOT NULL,
    PRIMARY KEY (provider, provider_uid)
);

-- Créée avant pin car pin la référence ; la FK vers images est ajoutée plus bas
CREATE TABLE voyage (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_user BIGINT NOT NULL REFERENCES users(id),
    titre text NOT NULL,
    date_debut date,
    date_fin date,
    id_image_cover BIGINT
);

CREATE TABLE pin (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_user BIGINT NOT NULL REFERENCES users(id),
    id_voyage BIGINT REFERENCES voyage(id),
    titre text NOT NULL,
    description_pin text,
    lieux text,
    country_code text,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    -- Point géographique calculé automatiquement depuis lat/long (pour les requêtes de distance PostGIS)
    geom GEOGRAPHY(Point, 4326) GENERATED ALWAYS AS (
        ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
    ) STORED,
    date_pin date NOT NULL DEFAULT CURRENT_DATE,
    visibilite visibilite_type NOT NULL,
    favori boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE images (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pin BIGINT NOT NULL REFERENCES pin(id),
    picture_url text NOT NULL,
    position smallint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE voyage
    ADD CONSTRAINT voyage_image_cover_fk FOREIGN KEY (id_image_cover) REFERENCES images(id);

CREATE TABLE liste_amis (
    id_user BIGINT NOT NULL REFERENCES users(id),
    id_friend BIGINT NOT NULL REFERENCES users(id),
    status varchar(8) NOT NULL CHECK (status IN ('Accepter', 'Refuser')),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (id_user, id_friend),
    CHECK (id_user <> id_friend)
);

CREATE TABLE pin_participant (
    id_pin BIGINT NOT NULL REFERENCES pin(id),
    id_user BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (id_pin, id_user)
);
