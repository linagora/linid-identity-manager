CREATE TABLE IF NOT EXISTS lemonldap_accounts (
    username VARCHAR(255) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    cn       VARCHAR(255),
    mail     VARCHAR(255),
    roles    VARCHAR(255)
);

INSERT INTO lemonldap_accounts (username, password, cn, mail, roles) VALUES ('admin', 'password', 'admin name', 'admin@example.com', 'admin,user');
INSERT INTO lemonldap_accounts (username, password, cn, mail, roles) VALUES ('user1', 'password', 'user1 name', 'user1@example.com', 'user');
INSERT INTO lemonldap_accounts (username, password, cn, mail, roles) VALUES ('user2', 'password', 'user2 name', 'user2@example.com', 'user');
-- Has a LinID account without any role, global or functional.
INSERT INTO lemonldap_accounts (username, password, cn, mail, roles) VALUES ('norole', 'password', 'norole name', 'norole@example.com', 'user');
INSERT INTO lemonldap_accounts (username, password, cn, mail, roles) VALUES ('noaccount', 'password', 'noaccount name', 'noaccount@example.com', 'user');
-- Carries the email of admin on purpose: no LinID account has its subject, so the API must reject its tokens.
INSERT INTO lemonldap_accounts (username, password, cn, mail, roles) VALUES ('impostor', 'password', 'impostor name', 'admin@example.com', 'user');
