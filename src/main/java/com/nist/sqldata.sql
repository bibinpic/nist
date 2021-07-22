CREATE TABLE public.people
(
    id integer NOT NULL,
    manager_id integer NOT NULL,
    username text COLLATE pg_catalog."default",
    display_name text COLLATE pg_catalog."default",
    CONSTRAINT people_pkey PRIMARY KEY (id),
    CONSTRAINT people_manager_id_fkey FOREIGN KEY (manager_id)
        REFERENCES public.people (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.people
    OWNER to postgres;
    
    
CREATE TABLE public.system1_usr
(
    system1_usr_id integer NOT NULL DEFAULT nextval('system1_usr_system1_usr_id_seq'::regclass),
    system1_displayname text COLLATE pg_catalog."default",
    CONSTRAINT system1_usr_pkey PRIMARY KEY (system1_usr_id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.system1_usr
    OWNER to postgres;
    
    
    CREATE TABLE public.system1_usr_grp_membership
(
    system1_usr_id integer NOT NULL,
    system1_grp_nm text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT system1_usr_grp_membership_pkey PRIMARY KEY (system1_usr_id, system1_grp_nm),
    CONSTRAINT fk_usr_grp_membership FOREIGN KEY (system1_usr_id)
        REFERENCES public.system1_usr (system1_usr_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.system1_usr_grp_membership
    OWNER to postgres;
    
    CREATE TABLE public.system2_usr
(
    system2_usr_id integer NOT NULL DEFAULT nextval('system2_usr_system2_usr_id_seq'::regclass),
    system2_displayname text COLLATE pg_catalog."default",
    CONSTRAINT system2_usr_pkey PRIMARY KEY (system2_usr_id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.system2_usr
    OWNER to postgres;
    
    
    CREATE TABLE public.system2_usr_grp_membership
(
    system2_usr_id integer NOT NULL,
    system2_grp_nm text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT system2_usr_grp_membership_pkey PRIMARY KEY (system2_usr_id, system2_grp_nm),
    CONSTRAINT fk_2usr_grp_membership FOREIGN KEY (system2_usr_id)
        REFERENCES public.system2_usr (system2_usr_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.system2_usr_grp_membership
    OWNER to postgres;