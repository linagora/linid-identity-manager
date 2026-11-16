Feature: Test API account recompute and synchronization cycle endpoints

  # Note: Background handles authentication before each Scenario.

  ################## Recompute (POST /accounts/{id}/recompute-rights) ##############
  ## 101 Should return 202 and start the recomputation of an existing account
  ## 102 Should return 404 for an unknown account id
  ## 103 Should answer that a recomputation is already running without starting a new one
  ## 104 Should store the payload of the account and its rights on each deployed application

  ################## Cycle (GET /sync/cycle) ##############
  ## 201 Should return the status of the synchronization cycle
  ## 202 Should answer that the cycle is running while the cycle lock is held

  Background:
    Given I set http header 'Authorization' with '{{ env.E2E_AUTH_TOKEN }}'
    And   I set http header 'Content-Type' with 'application/x-www-form-urlencoded'
    When  I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=admin&password=password&scope=openid email profile roles
      """
    Then  I expect status code is 200
    And   I set http header 'Authorization' with 'Bearer {{response.body.access_token}}'
    And   I set http header 'Content-Type' with 'application/json'

  ####################################################
  ################## Recompute #######################
  ####################################################

  Scenario: 101 - Should return 202 and start the recomputation of an existing account
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a003/recompute-rights' with method 'POST'
    Then I expect status code is 202
    And  I expect '{{response.body.alreadyRunning}}' is 'false'

  Scenario: 102 - Should return 404 for an unknown account id
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000/recompute-rights' with method 'POST'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And  I expect '{{response.body.status}}' is '404'

  Scenario: 103 - Should answer that a recomputation is already running without starting a new one
    # Takes the lock of sync-api: 3 is LockNamespaces.ACCOUNT_RECOMPUTE, and the key the hash code of the account UUID
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "sync_api"
    When  I execute sql request "SELECT pg_advisory_lock(3, -2147426301)"
    And   I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a003/recompute-rights' with method 'POST'
    Then  I expect status code is 202
    And   I expect '{{response.body.alreadyRunning}}' is 'true'
    And   I execute sql request "SELECT pg_advisory_unlock(3, -2147426301)"

  Scenario: 104 - Should store the payload of the account and its rights on each deployed application
    # Creates an account and two deployed applications, recomputes the rights of the account, then reads the payload
    # and the rights written by sync-api. The sync_api database is the one of the lock awaited after the recomputation
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "sync_api"

    # Creates the test account in the organizational unit Dept A2-1
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-recompute-104",
        "lastname": "Doe",
        "firstname": "John",
        "email": "recompute-104@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-0000000000b1",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    # Attaches the account to the group guests
    When  I request '{{env.E2E_API_URL}}/groups/00000000-0000-4000-8000-000000009003/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201

    # First application, which allows the account. Its claims template uses the payload, allow and roles: the raw tags
    # keep its Jinjava expressions away from the runner, which renders the same syntax
    When  I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-rights-allowed",
        "name": "E2E rights allowed",
        "type": "OIDC",
        "claimsTemplate": "{% raw %}{ \"sub\": {{ payload.external_id | tojson }}, \"email\": {{ payload.email | tojson }}, \"units\": {{ payload.organizational_units | tojson }}, \"allowed\": {{ allow | tojson }}, \"roles\": {{ roles | tojson }} }{% endraw %}"
      }
      """
    Then  I expect status code is 201
    And   I store 'allowedAppId' as '{{response.body.id}}' in context

    # Declares two roles: the rule below only gives editor, so admin must not be in the stored rights
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.allowedAppId}}/roles' with method 'POST' with body:
      """
      {
        "name": "editor"
      }
      """
    Then  I expect status code is 201

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.allowedAppId}}/roles' with method 'POST' with body:
      """
      {
        "name": "admin"
      }
      """
    Then  I expect status code is 201

    # Allows the account with its group, read in the source account_payload_view, and gives it the role editor with its
    # email, read in the source accounts
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.allowedAppId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups\nroles contains \"editor\" if input.email == \"recompute-104@example.com\"",
        "disabled": false
      }
      """
    Then  I expect status code is 201
    And   I store 'allowedRuleId' as '{{response.body.id}}' in context

    # Rules are created disabled: enabling the rule adds it to the script of the application
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.allowedAppId}}/rules/{{ctx.allowedRuleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups\nroles contains \"editor\" if input.email == \"recompute-104@example.com\"",
        "disabled": false
      }
      """
    Then  I expect status code is 200

    # Publishes the policy on OPA: sync-api only computes the rights on the deployed applications
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.allowedAppId}}/deploy' with method 'POST'
    Then  I expect status code is 200

    # Second application, which refuses the account, with another claims template
    When  I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-rights-denied",
        "name": "E2E rights denied",
        "type": "OIDC",
        "claimsTemplate": "{% raw %}{ \"uid\": {{ payload.external_id | tojson }}, \"groups\": {{ payload.groups | tojson }}, \"access\": {{ allow | tojson }}, \"profiles\": {{ roles | tojson }} }{% endraw %}"
      }
      """
    Then  I expect status code is 201
    And   I store 'deniedAppId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.deniedAppId}}/roles' with method 'POST' with body:
      """
      {
        "name": "viewer"
      }
      """
    Then  I expect status code is 201

    # Refuses the account, which is not in the group developers, and gives it the role viewer with its organizational
    # unit, read in the source account_payload_view
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.deniedAppId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"developers\" in input.groups\nroles contains \"viewer\" if \"Dept A2-1\" in input.organizational_units",
        "disabled": false
      }
      """
    Then  I expect status code is 201
    And   I store 'deniedRuleId' as '{{response.body.id}}' in context

    # Enables the rule, like on the first application
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.deniedAppId}}/rules/{{ctx.deniedRuleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"developers\" in input.groups\nroles contains \"viewer\" if \"Dept A2-1\" in input.organizational_units",
        "disabled": false
      }
      """
    Then  I expect status code is 200

    # Publishes the policy of the second application on OPA
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.deniedAppId}}/deploy' with method 'POST'
    Then  I expect status code is 200

    # Starts the job in the background: it builds the payload of the account, then computes its rights on both
    # applications
    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/recompute-rights' with method 'POST'
    Then  I expect status code is 202
    And   I expect '{{response.body.alreadyRunning}}' is 'false'

    # Waits for the end of the job, which holds its lock until then: 3 is LockNamespaces.ACCOUNT_RECOMPUTE, and the key
    # the hash code of the account UUID, which is the XOR of its four 32-bit words
    When  I execute sql request "SELECT (('x' || substr(id, 1, 8))::bit(32) # ('x' || substr(id, 9, 8))::bit(32) # ('x' || substr(id, 17, 8))::bit(32) # ('x' || substr(id, 25, 8))::bit(32))::int AS key FROM replace('{{ctx.accountId}}', '-', '') AS id"
    Then  I store 'lockKey' as '{{ctx.dbResults[0].key}}' in context
    # Blocks until the job releases the lock, then releases it at once
    When  I execute sql request "SELECT pg_advisory_lock(3, {{ctx.lockKey}})"
    And   I execute sql request "SELECT pg_advisory_unlock(3, {{ctx.lockKey}})"

    # sync-api writes the payloads and the rights in the linid database
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # The payload holds the values of the two sources of the mapping: external_id and email come from accounts,
    # organizational_units and groups from account_payload_view
    When  I execute sql request "SELECT payload ->> 'external_id' AS external_id, payload ->> 'email' AS email, payload -> 'organizational_units' ->> 0 AS organizational_unit, payload -> 'groups' ->> 0 AS group_code FROM account_payloads WHERE act_id = $1" with values:
      """
      [
        "{{ctx.accountId}}"
      ]
      """
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].external_id}}' is 'ext-recompute-104'
    And   I expect '{{ctx.dbResults[0].email}}' is 'recompute-104@example.com'
    And   I expect '{{ctx.dbResults[0].organizational_unit}}' is 'Dept A2-1'
    And   I expect '{{ctx.dbResults[0].group_code}}' is 'guests'

    # The first application allows the account with the role editor only. The claims are compared as the text of the
    # jsonb column, which sorts the keys by length. to_compute is false when the row holds the current checksums of the
    # payload, of the script and of the claims template
    When  I execute sql request "SELECT allow, roles::text AS roles, claims::text AS claims, to_compute FROM computed_rights JOIN account_computed_rights_view USING (act_id, app_id) WHERE act_id = $1 AND app_code = $2" with values:
      """
      [
        "{{ctx.accountId}}",
        "e2e-rights-allowed"
      ]
      """
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].allow}}' is 'true'
    And   I expect '{{ctx.dbResults[0].roles}}' is '["editor"]'
    And   I expect '{{ctx.dbResults[0].claims}}' is '{"sub": "ext-recompute-104", "email": "recompute-104@example.com", "roles": ["editor"], "units": ["Dept A2-1"], "allowed": true}'
    And   I expect '{{ctx.dbResults[0].to_compute}}' is 'false'

    # The second application refuses the account, with the role viewer, and renders its own claims
    When  I execute sql request "SELECT allow, roles::text AS roles, claims::text AS claims, to_compute FROM computed_rights JOIN account_computed_rights_view USING (act_id, app_id) WHERE act_id = $1 AND app_code = $2" with values:
      """
      [
        "{{ctx.accountId}}",
        "e2e-rights-denied"
      ]
      """
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].allow}}' is 'false'
    And   I expect '{{ctx.dbResults[0].roles}}' is '["viewer"]'
    And   I expect '{{ctx.dbResults[0].claims}}' is '{"uid": "ext-recompute-104", "access": false, "groups": ["guests"], "profiles": ["viewer"]}'
    And   I expect '{{ctx.dbResults[0].to_compute}}' is 'false'

    # Deletes the test data: the rights and the payload are deleted with their applications and their account
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.allowedAppId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.deniedAppId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  ####################################################
  ################## Cycle ###########################
  ####################################################

  Scenario: 201 - Should return the status of the synchronization cycle
    When I request '{{env.E2E_API_URL}}/sync/cycle' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.running}}' is 'false'

  Scenario: 202 - Should answer that the cycle is running while the cycle lock is held
    # Takes the cycle lock of sync-api: 1 is LockNamespaces.CYCLE, and 0 its only key
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "sync_api"
    When  I execute sql request "SELECT pg_advisory_lock(1, 0)"
    And   I request '{{env.E2E_API_URL}}/sync/cycle' with method 'GET'
    Then  I expect status code is 200
    And   I expect '{{response.body.running}}' is 'true'
    And   I execute sql request "SELECT pg_advisory_unlock(1, 0)"
