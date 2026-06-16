-- =============================================================
-- SEED DEMO DATA
-- =============================================================

-- All users are created by account admin
WITH generated AS (SELECT gen_random_uuid() AS admin_id),
     accounts_to_insert AS (
         SELECT admin_id AS act_id, 'admin' AS external_id, 'admin@example.com' AS email, 'Administrateur' AS lastname, 'Admin' AS firstname, admin_id AS created_by FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'jdupont',    'jean.dupont@example.com',       'Dupont',     'Jean',      admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'mmartin',    'marie.martin@example.com',      'Martin',     'Marie',     admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'pdurand',    'pierre.durand@example.com',     'Durand',     'Pierre',    admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'sbernard',   'sophie.bernard@example.com',    'Bernard',    'Sophie',    admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'tlambert',   'thomas.lambert@example.com',    'Lambert',    'Thomas',    admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'clefebvre',  'claire.lefebvre@example.com',   'Lefebvre',   'Claire',    admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'rmoreau',    'romain.moreau@example.com',     'Moreau',     'Romain',    admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'jrobert',    'julie.robert@example.com',      'Robert',     'Julie',     admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'nrichard',   'nicolas.richard@example.com',   'Richard',   'Nicolas',   admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'apetit',     'amelie.petit@example.com',      'Petit',      'Amélie',    admin_id FROM generated
         UNION ALL
         -- Additional users for demo
         SELECT gen_random_uuid(), 'lcolin',     'lucas.colin@example.com',       'Colin',      'Lucas',     admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'efournier',  'emma.fournier@example.com',     'Fournier',   'Emma',      admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'agirard',    'alexis.girard@example.com',     'Girard',     'Alexis',    admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'cboyer',     'camille.boyer@example.com',     'Boyer',      'Camille',   admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'nleroy',     'nathalie.leroy@example.com',    'Leroy',      'Nathalie',  admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'bsimon',     'baptiste.simon@example.com',    'Simon',      'Baptiste',  admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'imichel',    'isabelle.michel@example.com',   'Michel',     'Isabelle',  admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'ogarcia',    'olivier.garcia@example.com',    'Garcia',     'Olivier',   admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'ldavid',     'lea.david@example.com',         'David',      'Léa',       admin_id FROM generated
     ),
     inserted_accounts AS (
INSERT INTO accounts (act_id, external_id, email, lastname, firstname, payload, checksum, created_by, updated_by)
SELECT act_id,
       external_id,
       email,
       lastname,
       firstname,
       '{}'::jsonb,
    encode(digest('{}', 'sha256'), 'hex'),
       created_by,
       created_by
FROM accounts_to_insert
    ON CONFLICT (email) DO NOTHING
             RETURNING act_id, external_id, created_by
     )
INSERT INTO account_status (act_id, validity_period, created_by, updated_by)
SELECT act_id,
       tstzrange('2024-01-01 00:00:00+00'::timestamptz, NULL, '[)'),
       created_by,
       created_by
FROM inserted_accounts
-- Exclude admin account from status initialization to allow testing of 404 when no status account row exists yet
WHERE external_id != 'admin';


-- =============================================================
-- ORGANIZATIONAL UNIT TREE
-- =============================================================
DO $$
    DECLARE
root_id    UUID;
        admin_id   UUID;

        -- COMPANIES
        horizon_id UUID;
        nova_id    UUID;

        -- DIVISIONS - Groupe Horizon
        dir_com_id UUID;
        dir_tech_id UUID;

        -- DIVISIONS - Nova Services
        dir_rel_id UUID;
        dir_rh_id  UUID;

        -- DEPARTMENTS
        dep_ventes_id    UUID;
        dep_mkt_id       UUID;
        dep_dev_id       UUID;
        dep_infra_id     UUID;
        dep_support_id   UUID;
        dep_qualite_id   UUID;
        dep_recrutement_id UUID;

        -- TEAMS
        team_paris_id    UUID;
        team_lille_id    UUID;
        team_lyon_id     UUID;
        team_bordeaux_id UUID;
        team_nantes_id   UUID;
        team_toulouse_id UUID;

BEGIN
SELECT oun_id INTO root_id
FROM organizational_units
WHERE name = 'root'
    LIMIT 1;

SELECT act_id INTO admin_id
FROM accounts
WHERE email = 'admin@example.com'
    LIMIT 1;

-- =========================================================
-- 1. LEVEL 1 - COMPANIES
-- =========================================================
INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
VALUES
    (gen_random_uuid(), 'Groupe Horizon',  'COMPANY', admin_id, admin_id),
    (gen_random_uuid(), 'Nova Services',   'COMPANY', admin_id, admin_id)
    ON CONFLICT (type, name) DO NOTHING;

SELECT oun_id INTO horizon_id FROM organizational_units WHERE name = 'Groupe Horizon' AND type = 'COMPANY';
SELECT oun_id INTO nova_id    FROM organizational_units WHERE name = 'Nova Services'  AND type = 'COMPANY';

INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
VALUES
    (root_id, horizon_id, admin_id, admin_id),
    (root_id, nova_id,    admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- =========================================================
-- 2. LEVEL 2 - DIVISIONS
-- =========================================================
INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
VALUES
    (gen_random_uuid(), 'Direction Commerciale',       'DIVISION', admin_id, admin_id),
    (gen_random_uuid(), 'Direction Technique',         'DIVISION', admin_id, admin_id),
    (gen_random_uuid(), 'Direction Relation Client',   'DIVISION', admin_id, admin_id),
    (gen_random_uuid(), 'Direction Ressources Humaines','DIVISION', admin_id, admin_id)
    ON CONFLICT (type, name) DO NOTHING;

SELECT oun_id INTO dir_com_id  FROM organizational_units WHERE name = 'Direction Commerciale'         AND type = 'DIVISION';
SELECT oun_id INTO dir_tech_id FROM organizational_units WHERE name = 'Direction Technique'           AND type = 'DIVISION';
SELECT oun_id INTO dir_rel_id  FROM organizational_units WHERE name = 'Direction Relation Client'     AND type = 'DIVISION';
SELECT oun_id INTO dir_rh_id   FROM organizational_units WHERE name = 'Direction Ressources Humaines' AND type = 'DIVISION';

INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
VALUES
    (horizon_id, dir_com_id,  admin_id, admin_id),
    (horizon_id, dir_tech_id, admin_id, admin_id),
    (nova_id,    dir_rel_id,  admin_id, admin_id),
    (nova_id,    dir_rh_id,   admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- =========================================================
-- 3. LEVEL 3 - DEPARTMENTS
-- =========================================================
INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
VALUES
    (gen_random_uuid(), 'Ventes France',          'DEPARTMENT', admin_id, admin_id),
    (gen_random_uuid(), 'Marketing',              'DEPARTMENT', admin_id, admin_id),
    (gen_random_uuid(), 'Développement Logiciel', 'DEPARTMENT', admin_id, admin_id),
    (gen_random_uuid(), 'Infrastructure & Cloud', 'DEPARTMENT', admin_id, admin_id),
    (gen_random_uuid(), 'Support Client',         'DEPARTMENT', admin_id, admin_id),
    (gen_random_uuid(), 'Qualité & Process',      'DEPARTMENT', admin_id, admin_id),
    (gen_random_uuid(), 'Recrutement',            'DEPARTMENT', admin_id, admin_id)
    ON CONFLICT (type, name) DO NOTHING;

SELECT oun_id INTO dep_ventes_id     FROM organizational_units WHERE name = 'Ventes France'          AND type = 'DEPARTMENT';
SELECT oun_id INTO dep_mkt_id        FROM organizational_units WHERE name = 'Marketing'              AND type = 'DEPARTMENT';
SELECT oun_id INTO dep_dev_id        FROM organizational_units WHERE name = 'Développement Logiciel' AND type = 'DEPARTMENT';
SELECT oun_id INTO dep_infra_id      FROM organizational_units WHERE name = 'Infrastructure & Cloud' AND type = 'DEPARTMENT';
SELECT oun_id INTO dep_support_id    FROM organizational_units WHERE name = 'Support Client'         AND type = 'DEPARTMENT';
SELECT oun_id INTO dep_qualite_id    FROM organizational_units WHERE name = 'Qualité & Process'      AND type = 'DEPARTMENT';
SELECT oun_id INTO dep_recrutement_id FROM organizational_units WHERE name = 'Recrutement'           AND type = 'DEPARTMENT';

INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
VALUES
    (dir_com_id,  dep_ventes_id,      admin_id, admin_id),
    (dir_com_id,  dep_mkt_id,         admin_id, admin_id),
    (dir_tech_id, dep_dev_id,          admin_id, admin_id),
    (dir_tech_id, dep_infra_id,        admin_id, admin_id),
    (dir_rel_id,  dep_support_id,      admin_id, admin_id),
    (dir_rel_id,  dep_qualite_id,      admin_id, admin_id),
    (dir_rh_id,   dep_recrutement_id,  admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- =========================================================
-- 4. LEVEL 4 - TEAMS
-- =========================================================
INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
VALUES
    (gen_random_uuid(), 'Équipe Paris',    'TEAM', admin_id, admin_id),
    (gen_random_uuid(), 'Équipe Lille',    'TEAM', admin_id, admin_id),
    (gen_random_uuid(), 'Équipe Lyon',     'TEAM', admin_id, admin_id),
    (gen_random_uuid(), 'Équipe Bordeaux', 'TEAM', admin_id, admin_id),
    (gen_random_uuid(), 'Équipe Nantes',   'TEAM', admin_id, admin_id),
    (gen_random_uuid(), 'Équipe Toulouse', 'TEAM', admin_id, admin_id)
    ON CONFLICT (type, name) DO NOTHING;

SELECT oun_id INTO team_paris_id    FROM organizational_units WHERE name = 'Équipe Paris'    AND type = 'TEAM';
SELECT oun_id INTO team_lille_id    FROM organizational_units WHERE name = 'Équipe Lille'    AND type = 'TEAM';
SELECT oun_id INTO team_lyon_id     FROM organizational_units WHERE name = 'Équipe Lyon'     AND type = 'TEAM';
SELECT oun_id INTO team_bordeaux_id FROM organizational_units WHERE name = 'Équipe Bordeaux' AND type = 'TEAM';
SELECT oun_id INTO team_nantes_id   FROM organizational_units WHERE name = 'Équipe Nantes'   AND type = 'TEAM';
SELECT oun_id INTO team_toulouse_id FROM organizational_units WHERE name = 'Équipe Toulouse' AND type = 'TEAM';

INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
VALUES
    (dep_ventes_id,  team_paris_id,    admin_id, admin_id),
    (dep_mkt_id,     team_lille_id,    admin_id, admin_id),
    (dep_dev_id,     team_lyon_id,     admin_id, admin_id),
    (dep_support_id, team_bordeaux_id, admin_id, admin_id),
    (dep_infra_id,   team_nantes_id,   admin_id, admin_id),
    (dep_qualite_id, team_toulouse_id, admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- =========================================================
-- 5. MULTI-PARENT RELATIONS (DAG)
-- =========================================================

-- Équipe Lyon also reports to Marketing (cross-functional)
INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
VALUES (dep_mkt_id, team_lyon_id, admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- Équipe Bordeaux also reports to Software Development (cross-functional)
INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
VALUES (dep_dev_id, team_bordeaux_id, admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- Équipe Toulouse also reports to Recruitment (cross-functional)
INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
VALUES (dep_recrutement_id, team_toulouse_id, admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- =========================================================
-- 6. ASSIGN USERS TO ORGANIZATIONAL UNITS
-- =========================================================

-- Admin in root OU
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
VALUES (root_id, admin_id, admin_id, admin_id)
    ON CONFLICT DO NOTHING;

-- ── COMPANIES ──────────────────────────────────────────────
-- jdupont and mmartin at Groupe Horizon level (global membership)
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT horizon_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('jdupont', 'mmartin')
    ON CONFLICT DO NOTHING;

-- tlambert and clefebvre at Nova Services level (global membership)
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT nova_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('tlambert', 'clefebvre')
    ON CONFLICT DO NOTHING;

-- ── DIVISIONS ──────────────────────────────────────────────
-- pdurand: head of Direction Commerciale
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dir_com_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id = 'pdurand'
    ON CONFLICT DO NOTHING;

-- sbernard: head of Direction Technique
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dir_tech_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id = 'sbernard'
    ON CONFLICT DO NOTHING;

-- nleroy: head of Direction Relation Client
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dir_rel_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id = 'nleroy'
    ON CONFLICT DO NOTHING;

-- imichel: head of Direction Ressources Humaines
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dir_rh_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id = 'imichel'
    ON CONFLICT DO NOTHING;

-- ── DEPARTMENTS ────────────────────────────────────────────
-- jdupont and lcolin in Ventes France
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dep_ventes_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('jdupont', 'lcolin')
    ON CONFLICT DO NOTHING;

-- mmartin and efournier in Marketing
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dep_mkt_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('mmartin', 'efournier')
    ON CONFLICT DO NOTHING;

-- rmoreau, agirard and ogarcia in Développement Logiciel
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dep_dev_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('rmoreau', 'agirard', 'ogarcia')
    ON CONFLICT DO NOTHING;

-- bsimon and ldavid in Infrastructure & Cloud
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dep_infra_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('bsimon', 'ldavid')
    ON CONFLICT DO NOTHING;

-- jrobert and cboyer in Support Client
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dep_support_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('jrobert', 'cboyer')
    ON CONFLICT DO NOTHING;

-- nrichard in Qualité & Process
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dep_qualite_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id = 'nrichard'
    ON CONFLICT DO NOTHING;

-- imichel and apetit in Recrutement
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT dep_recrutement_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('imichel', 'apetit')
    ON CONFLICT DO NOTHING;

-- ── TEAMS ──────────────────────────────────────────────────
-- Équipe Paris: field sales
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT team_paris_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('jdupont', 'lcolin', 'cboyer')
    ON CONFLICT DO NOTHING;

-- Équipe Lille: marketing & communications
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT team_lille_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('mmartin', 'efournier', 'apetit')
    ON CONFLICT DO NOTHING;

-- Équipe Lyon: dev + cross-functional marketing
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT team_lyon_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('rmoreau', 'agirard', 'efournier')
    ON CONFLICT DO NOTHING;

-- Équipe Bordeaux: support + cross-functional dev
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT team_bordeaux_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('jrobert', 'ogarcia', 'rmoreau')
    ON CONFLICT DO NOTHING;

-- Équipe Nantes: infra & cloud
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT team_nantes_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('bsimon', 'ldavid', 'agirard')
    ON CONFLICT DO NOTHING;

-- Équipe Toulouse: quality + cross-functional recruitment
INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
SELECT team_toulouse_id, act_id, admin_id, admin_id
FROM accounts WHERE external_id IN ('nrichard', 'tlambert', 'clefebvre')
    ON CONFLICT DO NOTHING;

END
$$;