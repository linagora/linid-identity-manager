-- All users are created by account admin
WITH generated AS (SELECT gen_random_uuid() AS admin_id),
     accounts_to_insert AS (
         SELECT admin_id AS act_id, 'admin' AS external_id, 'admin@example.com' AS email, 'admin_ln' AS lastname, 'admin_fn' AS firstname, admin_id AS created_by FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user1', 'user1@example.com', 'user1_ln', 'user1_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user2', 'user2@example.com', 'user2_ln', 'user2_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user3', 'user3@example.com', 'user3_ln', 'user3_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user4', 'user4@example.com', 'user4_ln', 'user4_fn', admin_id FROM generated
         UNION ALL
         SELECT gen_random_uuid(), 'user5', 'user5@example.com', 'user5_ln', 'user5_fn', admin_id FROM generated
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
INSERT
INTO account_status (act_id, validity_period, created_by, updated_by)
SELECT act_id,
       tstzrange('2024-01-01 00:00:00+00'::timestamptz, NULL, '[)'),
       created_by,
       created_by
FROM inserted_accounts
-- Exclude admin account from status initialization to allow testing of 404 when no status account row exists yet
WHERE external_id != 'admin';

-- Create Organizational Unit tree
DO $$
    DECLARE root_id UUID;
    DECLARE admin_id UUID;
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
        -- 1. LEVEL 1 - COMPANIES (direct children of root)
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Company A', 'COMPANY', admin_id, admin_id),
            (gen_random_uuid(), 'Company B', 'COMPANY', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link root to companies
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT root_id, oun_id, admin_id, admin_id
        FROM organizational_units
        WHERE type = 'COMPANY';

        -- =========================================================
        -- 2. LEVEL 2 - DIVISIONS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Division A1', 'DIVISION', admin_id, admin_id),
            (gen_random_uuid(), 'Division A2', 'DIVISION', admin_id, admin_id),
            (gen_random_uuid(), 'Division B1', 'DIVISION', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link Company A to its divisions
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT c.oun_id, d.oun_id, admin_id, admin_id
        FROM organizational_units c
                 JOIN organizational_units d ON TRUE
        WHERE c.name = 'Company A'
          AND c.type = 'COMPANY'
          AND d.type = 'DIVISION'
          AND d.name IN ('Division A1', 'Division A2');

        -- Link Company B to its division
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT c.oun_id, d.oun_id, admin_id, admin_id
        FROM organizational_units c
                 JOIN organizational_units d ON TRUE
        WHERE c.name = 'Company B'
          AND c.type = 'COMPANY'
          AND d.type = 'DIVISION'
          AND d.name = 'Division B1';

        -- =========================================================
        -- 3. LEVEL 3 - DEPARTMENTS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Dept A1-1', 'DEPARTMENT', admin_id, admin_id),
            (gen_random_uuid(), 'Dept A1-2', 'DEPARTMENT', admin_id, admin_id),
            (gen_random_uuid(), 'Dept A2-1', 'DEPARTMENT', admin_id, admin_id),
            (gen_random_uuid(), 'Dept B1-1', 'DEPARTMENT', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link divisions to departments
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT d.oun_id, dep.oun_id, admin_id, admin_id
        FROM organizational_units d
                 JOIN organizational_units dep ON TRUE
        WHERE d.name = 'Division A1'
          AND dep.name IN ('Dept A1-1', 'Dept A1-2');

        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT d.oun_id, dep.oun_id, admin_id, admin_id
        FROM organizational_units d
                 JOIN organizational_units dep ON TRUE
        WHERE d.name = 'Division A2'
          AND dep.name = 'Dept A2-1';

        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT d.oun_id, dep.oun_id, admin_id, admin_id
        FROM organizational_units d
                 JOIN organizational_units dep ON TRUE
        WHERE d.name = 'Division B1'
          AND dep.name = 'Dept B1-1';

        -- =========================================================
        -- 4. LEVEL 4 - TEAMS
        -- =========================================================
        INSERT INTO organizational_units (oun_id, name, type, created_by, updated_by)
        VALUES
            (gen_random_uuid(), 'Team Alpha', 'TEAM', admin_id, admin_id),
            (gen_random_uuid(), 'Team Beta', 'TEAM', admin_id, admin_id),
            (gen_random_uuid(), 'Team Gamma', 'TEAM', admin_id, admin_id),
            (gen_random_uuid(), 'Team Delta', 'TEAM', admin_id, admin_id)
        ON CONFLICT (type, name) DO NOTHING;

        -- Link departments to teams
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT dep.oun_id, t.oun_id, admin_id, admin_id
        FROM organizational_units dep
                 JOIN organizational_units t ON TRUE
        WHERE (dep.name = 'Dept A1-1' AND t.name = 'Team Alpha')
           OR (dep.name = 'Dept A1-2' AND t.name = 'Team Beta')
           OR (dep.name = 'Dept A2-1' AND t.name = 'Team Gamma')
           OR (dep.name = 'Dept B1-1' AND t.name = 'Team Delta');

        -- =========================================================
        -- 5. MULTI-PARENT RELATIONS (DAG stress test cases)
        -- =========================================================

        -- Team Gamma has two parents (multi-parent scenario)
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT dep.oun_id, t.oun_id, admin_id, admin_id
        FROM organizational_units dep
                 JOIN organizational_units t ON TRUE
        WHERE dep.name = 'Dept A1-2'
          AND t.name = 'Team Gamma';

        -- Team Delta has two parents (multi-parent scenario)
        INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by)
        SELECT dep.oun_id, t.oun_id, admin_id, admin_id
        FROM organizational_units dep
                 JOIN organizational_units t ON TRUE
        WHERE dep.name = 'Dept A2-1'
          AND t.name = 'Team Delta';


        -- =========================================================
        -- 6. Add user inside OU
        -- =========================================================
        -- Admin in OU root
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (root_id, admin_id, admin_id, admin_id);

        -- user1 in OU Company A
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Company A'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user1'),
                   admin_id,
                   admin_id
               );

        -- user2 in OU Company B
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Company B'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user2'),
                   admin_id,
                   admin_id
               );

        -- user3 in OU Division A1
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Division A1'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user3'),
                   admin_id,
                   admin_id
               );

        -- user4 in OU Division A2
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Division A2'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user4'),
                   admin_id,
                   admin_id
               );

        -- user5 in OU Division B1
        INSERT INTO organizational_unit_accounts (oun_id, act_id, created_by, updated_by)
        VALUES (
                   (SELECT oun_id FROM organizational_units WHERE name = 'Division B1'),
                   (SELECT act_id FROM accounts WHERE external_id = 'user5'),
                   admin_id,
                   admin_id
               );
    END $$;
