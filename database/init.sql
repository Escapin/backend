CREATE TABLE users(
    id BIGINT identity unique not null PRIMARY KEY,
    pseudo text unique not null,
    email text,
    password_hash text,
    name text,
    surname text,
    ville_origine text ,
    url_photo_profil text,
    url_photo_fond_profil text,
    cgu_accepted_at timestamptz not null,
    created_at timestamptz not null default now()
);

CREATE TABLE pin(
    id BIGINT IDENTITY not null PRIMARY KEY,
    id_user bigint not null, //fk
    id_voyage bigint, //fk
    titre text not null,
    description text,
    lieux text,
    country_code text,
    latitude double not null,
    longitude double not null,
    geom GEOGRAPHY(Point, 4326) not null,
    date DATETIME2 not null default CURRENT_DATE,
    visibilite visibilite_type not null, //faire une enum
    favori boolean not null DEFAULT false,
    created_at timestamptz not null default now()
);

CREATE TABLE images(
    id BIGINT IDENTITY not null PRIMARY KEY,
    id_pin bigint not null, //FOREIGN KEY
    picture_url text not null,
    position smallint not null DEFAULT 0,
    created_at timestamptz not null default now()
);

CREATE TABLE liste_amis(
    id_user bigint not null, //FOREIGN KEY
    id_friend bigint not null, //FOREIGN KEY
    status friend_status not null, //enum
    created_at timestamptz not null default now()
);

CREATE TABLE pin_participant(
    id_pin bigint not null, //FOREIGN KEY
    id_user bigint not, //FOREIGN KEY
);

CREATE TABLE auth_provider(
    id_user BIGINT not null, //FOREIGN KEY
    provider provider_type not null, //enum
    provider_uid text not null
);

CREATE TABLE voyage(
    id bigint IDENTITY not null PRIMARY KEY,
    id_user bigint not null, //FOREIGN KEY
    titre text not null,
    date_debut datetime2,
    date_fin datetime2,
    id_image_cover bigint //FOREIGN KEY
);