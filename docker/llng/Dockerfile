FROM lemonldapng/lemonldap-ng

RUN apt-get update &&\
    apt-get install -y gettext-base

COPY entrypoint.sh /custom-entrypoint.sh
ENTRYPOINT ["/bin/sh", "/custom-entrypoint.sh"]
