# init_guest_role.py
from superset import security_manager, db

ROLE_NAME = "Guest"

PERMISSIONS = [
    ("can_read", "Dashboard"),
    ("can_read", "Chart"),
    ("can_read", "CurrentUserRestApi"),
    ("can_explore", "Superset"),
    ("can_explore_json", "Superset"),
    ("can_read", "Dataset"),
    ("can_list", "Dataset"),
    ("can_get", "Dataset"),
    ("can_read", "Database"),
    ("can_list", "Database"),
    ("can_get", "Database"),
    ("can_time_range", "Api"),
    ("all_datasource_access", "all_datasource_access")
]

role = security_manager.find_role(ROLE_NAME)
if not role:
    role = security_manager.add_role(ROLE_NAME)
    print(f"Role '{ROLE_NAME}' created.")

added = 0
for perm_name, view_name in PERMISSIONS:
    pvm = security_manager.find_permission_view_menu(perm_name, view_name)
    if pvm is None:
        pvm = security_manager.add_permission_view_menu(perm_name, view_name)
    if pvm not in role.permissions:
        role.permissions.append(pvm)
        added += 1

BROKER_ROLE_NAME = "GuestTokenIssuer"
BROKER_PERMISSIONS = [
    ("can_grant_guest_token", "SecurityRestApi"),
    ("can_read", "SecurityRestApi"),
    ("can_read", "Dashboard"),
    ("can_get_embedded", "Dashboard"),
    ("can_read", "EmbeddedDashboard")
]

broker_role = security_manager.find_role(BROKER_ROLE_NAME)
if not broker_role:
    broker_role = security_manager.add_role(BROKER_ROLE_NAME)

for perm_name, view_name in BROKER_PERMISSIONS:
    pvm = security_manager.find_permission_view_menu(perm_name, view_name)
    if pvm is None:
        pvm = security_manager.add_permission_view_menu(perm_name, view_name)
    if pvm not in broker_role.permissions:
        broker_role.permissions.append(pvm)

db.session.commit()
