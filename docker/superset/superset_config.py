import os
from datetime import timedelta

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
}

GUEST_ROLE_NAME = "Guest"

# JWT access token lifetime for the Superset API. The API's Caffeine cache
# for this token (SUPERSET_CACHE_OPTION) is configured with a TTL of 55min,
# slightly shorter than this default of 1h, so the token is renewed before
# it expires server-side.
JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=1)
