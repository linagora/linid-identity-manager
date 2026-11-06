Feature: Test API application recompute endpoint

  # Note: Background handles authentication before each Scenario.

  ################## Recompute (POST /applications/{id}/recompute-rights) ##############
  ## 101 Should return 202 and start the recomputation of a deployed application
  ## 102 Should return 400 for an application without script
  ## 103 Should return 400 for an application with a script that is not deployed
  ## 104 Should return 404 for an unknown application id
  ## 105 Should answer that a recomputation is already running without starting a new one
  ## 106 Should store the payloads of the accounts and the rights on the recomputed application only

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

  Scenario: 101 - Should return 202 and start the recomputation of a deployed application
    # The sync_api database is the one of the lock awaited after the recomputation
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "sync_api"

    When  I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-application-recompute-101",
        "name": "E2E application recompute 101",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then  I expect status code is 201
    And   I store 'applicationId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups",
        "disabled": false
      }
      """
    Then  I expect status code is 201
    And   I store 'ruleId' as '{{response.body.id}}' in context

    # Rules are created disabled: enabling the rule adds it to the script of the application
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups",
        "disabled": false
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/deploy' with method 'POST'
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/recompute-rights' with method 'POST'
    Then  I expect status code is 202
    And   I expect '{{response.body.alreadyRunning}}' is 'false'

    # Waits for the end of the job, which holds its lock until then: 4 is LockNamespaces.APPLICATION_RECOMPUTE, and the
    # key the hash code of the application UUID, which is the XOR of its four 32-bit words
    When  I execute sql request "SELECT (('x' || substr(id, 1, 8))::bit(32) # ('x' || substr(id, 9, 8))::bit(32) # ('x' || substr(id, 17, 8))::bit(32) # ('x' || substr(id, 25, 8))::bit(32))::int AS key FROM replace('{{ctx.applicationId}}', '-', '') AS id"
    Then  I store 'lockKey' as '{{ctx.dbResults[0].key}}' in context
    # Blocks until the job releases the lock, then releases it at once
    When  I execute sql request "SELECT pg_advisory_lock(4, {{ctx.lockKey}})"
    And   I execute sql request "SELECT pg_advisory_unlock(4, {{ctx.lockKey}})"

    And   I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 102 - Should return 400 for an application without script
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-application-recompute-102",
        "name": "E2E application recompute 102",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'applicationId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/recompute-rights' with method 'POST'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_deployed'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 103 - Should return 400 for an application with a script that is not deployed
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-application-recompute-103",
        "name": "E2E application recompute 103",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'applicationId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    # Rules are created disabled: enabling the rule adds it to the script of the application, which is not deployed
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups",
        "disabled": false
      }
      """
    Then I expect status code is 200

    # The application has a script: the 400 below only comes from its null deployedAt
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.hasScript}}' is 'true'
    And  I expect '{{response.body.deployedAt}}' is empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/recompute-rights' with method 'POST'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_deployed'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 104 - Should return 404 for an unknown application id
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-4000-8000-000000000000/recompute-rights' with method 'POST'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'
    And  I expect '{{response.body.status}}' is '404'

  Scenario: 105 - Should answer that a recomputation is already running without starting a new one
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "sync_api"

    When  I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-application-recompute-105",
        "name": "E2E application recompute 105",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then  I expect status code is 201
    And   I store 'applicationId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups",
        "disabled": false
      }
      """
    Then  I expect status code is 201
    And   I store 'ruleId' as '{{response.body.id}}' in context

    # Rules are created disabled: enabling the rule adds it to the script of the application
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups",
        "disabled": false
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/deploy' with method 'POST'
    Then  I expect status code is 200

    # Takes the lock of sync-api: 4 is LockNamespaces.APPLICATION_RECOMPUTE, and the key the hash code of the application
    # UUID, which is the XOR of its four 32-bit words
    When  I execute sql request "SELECT (('x' || substr(id, 1, 8))::bit(32) # ('x' || substr(id, 9, 8))::bit(32) # ('x' || substr(id, 17, 8))::bit(32) # ('x' || substr(id, 25, 8))::bit(32))::int AS key FROM replace('{{ctx.applicationId}}', '-', '') AS id"
    Then  I store 'lockKey' as '{{ctx.dbResults[0].key}}' in context
    When  I execute sql request "SELECT pg_advisory_lock(4, {{ctx.lockKey}})"
    And   I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/recompute-rights' with method 'POST'
    Then  I expect status code is 202
    And   I expect '{{response.body.alreadyRunning}}' is 'true'
    And   I execute sql request "SELECT pg_advisory_unlock(4, {{ctx.lockKey}})"

    # No job was started: nothing to wait for before deleting the application
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 106 - Should store the payloads of the accounts and the rights on the recomputed application only
    # Creates an account and two deployed applications, recomputes the rights on the first one, then reads the payload
    # and the rights written by sync-api. The sync_api database is the one of the lock awaited after the recomputation
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "sync_api"

    # Creates the test account in the organizational unit Dept A2-1
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-application-recompute-106",
        "lastname": "Doe",
        "firstname": "John",
        "email": "application-recompute-106@example.com",
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

    # Application recomputed. Its claims template uses the payload, allow and roles: the raw tags keep its Jinjava
    # expressions away from the runner, which renders the same syntax
    When  I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-application-recomputed",
        "name": "E2E application recomputed",
        "type": "OIDC",
        "claimsTemplate": "{% raw %}{ \"sub\": {{ payload.external_id | tojson }}, \"email\": {{ payload.email | tojson }}, \"units\": {{ payload.organizational_units | tojson }}, \"allowed\": {{ allow | tojson }}, \"roles\": {{ roles | tojson }} }{% endraw %}"
      }
      """
    Then  I expect status code is 201
    And   I store 'recomputedAppId' as '{{response.body.id}}' in context

    # Declares two roles: the rule below only gives editor, so admin must not be in the stored rights
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.recomputedAppId}}/roles' with method 'POST' with body:
      """
      {
        "name": "editor"
      }
      """
    Then  I expect status code is 201

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.recomputedAppId}}/roles' with method 'POST' with body:
      """
      {
        "name": "admin"
      }
      """
    Then  I expect status code is 201

    # Allows the account with its group, read in the source account_payload_view, and gives it the role editor with its
    # email, read in the source accounts
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.recomputedAppId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups\nroles contains \"editor\" if input.email == \"application-recompute-106@example.com\"",
        "disabled": false
      }
      """
    Then  I expect status code is 201
    And   I store 'recomputedRuleId' as '{{response.body.id}}' in context

    # Rules are created disabled: enabling the rule adds it to the script of the application
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.recomputedAppId}}/rules/{{ctx.recomputedRuleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups\nroles contains \"editor\" if input.email == \"application-recompute-106@example.com\"",
        "disabled": false
      }
      """
    Then  I expect status code is 200

    # Publishes the policy on OPA: sync-api only computes the rights on the deployed applications
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.recomputedAppId}}/deploy' with method 'POST'
    Then  I expect status code is 200

    # Other application, deployed too but not recomputed: it would give the account the role viewer
    When  I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "e2e-application-not-recomputed",
        "name": "E2E application not recomputed",
        "type": "OIDC",
        "claimsTemplate": "{% raw %}{ \"uid\": {{ payload.external_id | tojson }}, \"profiles\": {{ roles | tojson }} }{% endraw %}"
      }
      """
    Then  I expect status code is 201
    And   I store 'otherAppId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.otherAppId}}/roles' with method 'POST' with body:
      """
      {
        "name": "viewer"
      }
      """
    Then  I expect status code is 201

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.otherAppId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups\nroles contains \"viewer\" if \"Dept A2-1\" in input.organizational_units",
        "disabled": false
      }
      """
    Then  I expect status code is 201
    And   I store 'otherRuleId' as '{{response.body.id}}' in context

    # Enables the rule, like on the first application
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.otherAppId}}/rules/{{ctx.otherRuleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if \"guests\" in input.groups\nroles contains \"viewer\" if \"Dept A2-1\" in input.organizational_units",
        "disabled": false
      }
      """
    Then  I expect status code is 200

    # Publishes the policy of the other application on OPA
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.otherAppId}}/deploy' with method 'POST'
    Then  I expect status code is 200

    # Starts the job in the background: it builds the payloads of all the accounts, then computes the rights on the
    # first application only
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.recomputedAppId}}/recompute-rights' with method 'POST'
    Then  I expect status code is 202
    And   I expect '{{response.body.alreadyRunning}}' is 'false'

    # Waits for the end of the job, which holds its lock until then: 4 is LockNamespaces.APPLICATION_RECOMPUTE, and the
    # key the hash code of the application UUID, which is the XOR of its four 32-bit words
    When  I execute sql request "SELECT (('x' || substr(id, 1, 8))::bit(32) # ('x' || substr(id, 9, 8))::bit(32) # ('x' || substr(id, 17, 8))::bit(32) # ('x' || substr(id, 25, 8))::bit(32))::int AS key FROM replace('{{ctx.recomputedAppId}}', '-', '') AS id"
    Then  I store 'lockKey' as '{{ctx.dbResults[0].key}}' in context
    # Blocks until the job releases the lock, then releases it at once
    When  I execute sql request "SELECT pg_advisory_lock(4, {{ctx.lockKey}})"
    And   I execute sql request "SELECT pg_advisory_unlock(4, {{ctx.lockKey}})"

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
    And   I expect '{{ctx.dbResults[0].external_id}}' is 'ext-application-recompute-106'
    And   I expect '{{ctx.dbResults[0].email}}' is 'application-recompute-106@example.com'
    And   I expect '{{ctx.dbResults[0].organizational_unit}}' is 'Dept A2-1'
    And   I expect '{{ctx.dbResults[0].group_code}}' is 'guests'

    # The recomputed application allows the account with the role editor only. The claims are compared as the text of
    # the jsonb column, which sorts the keys by length. to_compute is false when the row holds the current checksums of
    # the payload, of the script and of the claims template
    When  I execute sql request "SELECT allow, roles::text AS roles, claims::text AS claims, to_compute FROM computed_rights JOIN account_computed_rights_view USING (act_id, app_id) WHERE act_id = $1 AND app_id = $2" with values:
      """
      [
        "{{ctx.accountId}}",
        "{{ctx.recomputedAppId}}"
      ]
      """
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].allow}}' is 'true'
    And   I expect '{{ctx.dbResults[0].roles}}' is '["editor"]'
    And   I expect '{{ctx.dbResults[0].claims}}' is '{"sub": "ext-application-recompute-106", "email": "application-recompute-106@example.com", "roles": ["editor"], "units": ["Dept A2-1"], "allowed": true}'
    And   I expect '{{ctx.dbResults[0].to_compute}}' is 'false'

    # The job only computes the pairs of its application
    When  I execute sql request "SELECT cpr_id FROM computed_rights WHERE act_id = $1 AND app_id = $2" with values:
      """
      [
        "{{ctx.accountId}}",
        "{{ctx.otherAppId}}"
      ]
      """
    Then  I expect 0 database results

    # Deletes the test data: the rights and the payload are deleted with their applications and their account
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.recomputedAppId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.otherAppId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204
