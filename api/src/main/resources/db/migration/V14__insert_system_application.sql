INSERT INTO applications (code, name, description, domain, type, claims_template, claims_template_checksum)
SELECT 'LINID',
       'LINID - Identity Manager',
       'System identity manager application',
       'Security',
       'System',
       claims_template,
       encode(sha256(convert_to(claims_template, 'UTF8')), 'hex')
FROM (VALUES ('{
  "sub": {{ payload.external_id | tojson }},
  "email": {{ payload.email | tojson }},
  "roles": {{ roles | tojson }}
}')) AS linid (claims_template);

INSERT INTO application_roles (app_id, name, description)
SELECT app_id,
       'Administrator',
       'System administrator role'
FROM applications
WHERE code = 'LINID';
