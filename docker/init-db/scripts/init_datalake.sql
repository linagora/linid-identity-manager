-- ============================================================================
-- Authentication events table
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DROP TABLE IF EXISTS authentication_events;

CREATE TABLE authentication_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_date TIMESTAMPTZ NOT NULL,
    rls_id TEXT NOT NULL,
    authentication_source VARCHAR(50) NOT NULL,
    authentication_method VARCHAR(50),
    client_id VARCHAR(255),
    target_application VARCHAR(255),
    ip_address INET,
    success BOOLEAN NOT NULL,
    session_id VARCHAR(255),
    extra_parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_authentication_events_date
    ON authentication_events(event_date);

CREATE INDEX idx_authentication_events_rls_id
    ON authentication_events(rls_id);

CREATE INDEX idx_authentication_events_success
    ON authentication_events(success);

-- ============================================================================
-- Demo data generation
-- ============================================================================

WITH users AS (
    SELECT *
    FROM (
             VALUES
                 ('admin'),
                 ('user1'),
                 ('user2'),
                 ('user3'),
                 ('user4'),
                 ('user5'),
                 ('user6'),
                 ('user7'),
                 ('user8'),
                 ('user9'),
                 ('user10'),
                 ('dialog-d1'),
                 ('dialog-d2'),
                 ('dialog-d3'),
                 ('dialog-d4'),
                 ('dialog-d5'),
                 ('dialog-d6'),
                 ('dialog-d7'),
                 ('dialog-d8'),
                 ('dialog-d9')
         ) u(rls_id)
),
     days AS (
         SELECT day
         FROM generate_series(
                              CURRENT_DATE - INTERVAL '365 days',
                              CURRENT_DATE,
                              INTERVAL '1 day'
              ) AS day
     ),

-- 80% des jours sont actifs globalement
     active_days AS (
         SELECT day
         FROM days
         WHERE random() > 0.20
     ),

-- Sélection des utilisateurs actifs pour chaque jour
     active_users AS (
         SELECT
             d.day,
             u.rls_id
         FROM active_days d
                  CROSS JOIN users u
         WHERE random() < 0.25
     ),

-- Nombre d'événements générés pour chaque utilisateur actif
     user_events AS (
         SELECT
             au.day,
             au.rls_id,
             gs.event_number
         FROM active_users au
                  CROSS JOIN generate_series(
                 1,
                 (1 + floor(random() * 8))::int
                             ) gs(event_number)
     ),

     generated_events AS (
         SELECT
             gen_random_uuid() AS id,

             (
                 ue.day
                     + ((8 + floor(random() * 11))::int || ' hours')::interval
                     + ((floor(random() * 60))::int || ' minutes')::interval
                     + ((floor(random() * 60))::int || ' seconds')::interval
                 )::timestamptz AS event_date,

             ue.rls_id,

             'LEMONLDAP' AS authentication_source,

             CASE
                 WHEN random() < 0.85 THEN 'OIDC'
                 ELSE 'SAML'
                 END AS authentication_method,

             app.client_id,

             app.target_application,

             (
                 '192.168.'
                     || floor(random() * 10)::int
                     || '.'
                     || floor(random() * 255)::int
                 )::inet AS ip_address,

             random() > 0.05 AS success,

             md5(random()::text) AS session_id,

             jsonb_build_object(
                     'browser',
                     (
                         ARRAY[
                             'Chrome',
                             'Firefox',
                             'Edge',
                             'Safari'
                             ]
                         )[floor(random() * 4 + 1)::int],

                     'os',
                     (
                         ARRAY[
                             'Windows',
                             'Linux',
                             'macOS'
                             ]
                         )[floor(random() * 3 + 1)::int]
             ) AS extra_parameters,

             NOW() AS insert_date

         FROM user_events ue

                  CROSS JOIN LATERAL (
             SELECT *
             FROM (
                      VALUES
                          ('linid-ui', 'LinID'),
                          ('grafana', 'Grafana'),
                          ('superset', 'Superset'),
                          ('gitlab', 'GitLab')
                  ) applications(client_id, target_application)
             ORDER BY random()
             LIMIT 1
             ) app
     )

INSERT INTO authentication_events
SELECT *
FROM generated_events;
