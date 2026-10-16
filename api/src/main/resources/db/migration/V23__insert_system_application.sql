INSERT INTO applications (code, name, description, domain, type, claims_template, claims_template_checksum)
VALUES ('LINID',
        'LINID - Identity Manager',
        'System identity manager application',
        'Security',
        'System',
        '{ "sub": "id" }',
        encode(sha256(convert_to('{ "sub": "id" }', 'UTF8')), 'hex'));

INSERT INTO application_roles (app_id, name, description)
SELECT app_id,
       'Administrator',
       'System administrator role'
FROM applications
WHERE code = 'LINID';
