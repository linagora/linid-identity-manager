FROM apache/superset:6.1.0

USER root

RUN apt-get update \
    && apt-get install -y \
        gcc \
        gettext-base \
        zip \
    && rm -rf /var/lib/apt/lists/*

RUN cd /app && . /app/.venv/bin/activate && \
    uv pip install \
        flask-cors \
        psycopg2 \
        Authlib

COPY superset_config.py /app/pythonpath/superset_config.py
COPY init_superset.sh /init_superset.sh

RUN chmod +x /init_superset.sh

USER superset

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=30s \
  CMD curl -skf https://localhost:8443/health || exit 1

ENTRYPOINT ["/init_superset.sh"]
