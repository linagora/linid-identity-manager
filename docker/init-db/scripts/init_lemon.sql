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
