import os

# ============================================================================
# Superset metadata database
# ============================================================================

SECRET_KEY = os.environ['SUPERSET_SECRET_KEY']

SQLALCHEMY_DATABASE_URI = (
    f"postgresql+psycopg2://"
    f"{os.environ['DATABASE_USER']}:"
    f"{os.environ['DATABASE_PASSWORD']}@"
    f"{os.environ['DATABASE_HOST']}:"
    f"{os.environ['DATABASE_PORT']}/"
    f"{os.environ['DATABASE_NAME']}"
)

ENABLE_VIEWERS = True
FEATURE_FLAGS = {"EMBEDDED_SUPERSET": True}

ENABLE_CORS = True
CORS_OPTIONS = {
    "origins": [os.environ['APPLICATION_URL']],
    "supports_credentials": True,
}

TALISMAN_ENABLED = True
TALISMAN_CONFIG = {
    "content_security_policy": {
        "frame-ancestors": ["'self'", os.environ['APPLICATION_URL']],
    },
    "force_https": False,
    "session_cookie_secure": False,
}

GUEST_ROLE_NAME = "Guest"