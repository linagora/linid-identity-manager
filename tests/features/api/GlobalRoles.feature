Feature: Test API Global role endpoints

  # Note: Background authenticates as user1, an account without any global role:
  #       these endpoints do not restrict access, every authenticated user can call them.
  # Each scenario creates its own account, and deletes it at the end;
  # deleting an account cascades to its global roles.

  ################## Create (POST /accounts/{accountId}/global-roles/{role}) ##################
  ## 101 Should assign a global role to an account and audit the insert
  ## 102 Should assign several global roles to the same account
  ## 103 Should return 400 when the account already holds the role
  ## 104 Should return 400 when the role is unknown
  ## 105 Should return 400 when the role is SUPER_ADMIN
  ## 106 Should return 404 when the account does not exist

  ################## Find (GET /global-roles, GET /accounts/{accountId}/global-roles) ##################
  ## 201 Should return the global roles with the identity of their account
  ## 202 Should filter the global roles by role
  ## 203 Should return the global roles of an account only
  ## 204 Should return 404 when listing the global roles of an account that does not exist
  ## 205 Should return 206 when the page does not hold all the global roles

  ################## Delete (DELETE /accounts/{accountId}/global-roles/{role}) ##################
  ## 301 Should remove a global role from the given account and audit the delete
  ## 302 Should return 404 when the account does not hold the role
  ## 303 Should return 404 when the account does not exist
  ## 304 Should return 400 when the role is unknown
  ## 305 Should remove the global roles of an account when the account is deleted

  Background:
    Given I set http header 'Authorization' with '{{ env.E2E_AUTH_TOKEN }}'
    And   I set http header 'Content-Type' with 'application/x-www-form-urlencoded'
    When  I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=user1&password=password&scope=openid email profile roles
      """
    Then  I expect status code is 200
    And   I store 'accessToken' as '{{response.body.access_token}}' in context
    And   I set http header 'Authorization' with 'Bearer {{ctx.accessToken}}'
    And   I set http header 'Content-Type' with 'application/json'

    When I request '{{env.E2E_API_URL}}/organizational-units?name=root&type=root' with method 'GET'
    Then I expect status code is 200
    And  I store 'rootId' as '{{response.body.content[0].id}}' in context

  ###############################################################
  ######## Create (POST /accounts/{accountId}/global-roles/{role}) ########
  ###############################################################

  Scenario: 101 - Should assign a global role to an account and audit the insert
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-101",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-101@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ACCOUNTS_ADMIN' with method 'POST'
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.role}}' is 'ACCOUNTS_ADMIN'
    And  I expect '{{response.body.createdBy}}' is '00000000-0000-4000-8000-00000000a002'
    And  I expect '{{response.body.updatedBy}}' is '00000000-0000-4000-8000-00000000a002'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty
    And  I store 'globalRoleId' as '{{response.body.id}}' in context

    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "SELECT operation, payload->'global_roles'->>'role' AS role, payload->'account'->>'email' AS email, payload->'changed_by'->>'email' AS changed_by FROM global_roles_audit WHERE gro_id = '{{ctx.globalRoleId}}'"
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].operation}}' is 'INSERT'
    And   I expect '{{ctx.dbResults[0].role}}' is 'ACCOUNTS_ADMIN'
    And   I expect '{{ctx.dbResults[0].email}}' is 'john-gro-101@example.com'
    And   I expect '{{ctx.dbResults[0].changed_by}}' is 'user1@example.com'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 102 - Should assign several global roles to the same account
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-102",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-102@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/GROUPS_ADMIN' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/GROUPS_AUDITOR' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '2'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 103 - Should return 400 when the account already holds the role
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-103",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-103@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ROLES_ADMIN' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ROLES_ADMIN' with method 'POST'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.global_role.already_assigned'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 104 - Should return 400 when the role is unknown
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a003/global-roles/UNKNOWN_ROLE' with method 'POST'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.global_role.unknown'

  Scenario: 105 - Should return 400 when the role is SUPER_ADMIN
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a003/global-roles/SUPER_ADMIN' with method 'POST'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.global_role.unknown'

  Scenario: 106 - Should return 404 when the account does not exist
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000/global-roles/ACCOUNTS_AUDITOR' with method 'POST'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  ###############################################################
  ################## Find (GET /global-roles, GET /accounts/{accountId}/global-roles) ##################
  ###############################################################

  Scenario: 201 - Should return the global roles with the identity of their account
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-201",
        "lastname": "Martin",
        "firstname": "Alice",
        "email": "alice-gro-201@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/APPLICATIONS_ADMIN' with method 'POST'
    Then I expect status code is 201
    And  I store 'globalRoleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/global-roles?accountId={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.globalRoleId}}'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.content[0].firstname}}' is 'Alice'
    And  I expect '{{response.body.content[0].lastname}}' is 'Martin'
    And  I expect '{{response.body.content[0].email}}' is 'alice-gro-201@example.com'
    And  I expect '{{response.body.content[0].role}}' is 'APPLICATIONS_ADMIN'
    And  I expect '{{response.body.content[0].createdBy}}' is 'user1_fn user1_ln'
    And  I expect '{{response.body.content[0].updatedBy}}' is 'user1_fn user1_ln'
    And  I expect '{{response.body.content[0].insertDate}}' is not empty
    And  I expect '{{response.body.content[0].updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 202 - Should filter the global roles by role
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-202",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-202@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ORGANIZATIONAL_UNITS_ADMIN' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ORGANIZATIONAL_UNITS_AUDITOR' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/global-roles?accountId={{ctx.accountId}}&role=ORGANIZATIONAL_UNITS_AUDITOR' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].role}}' is 'ORGANIZATIONAL_UNITS_AUDITOR'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 203 - Should return the global roles of an account only
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-203a",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-203a@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-203b",
        "lastname": "Doe",
        "firstname": "Jane",
        "email": "jane-gro-203b@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'otherAccountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ROLES_ADMIN' with method 'POST'
    Then I expect status code is 201
    And  I store 'globalRoleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.otherAccountId}}/global-roles/ROLES_AUDITOR' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.globalRoleId}}'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.content[0].role}}' is 'ROLES_ADMIN'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.otherAccountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 204 - Should return 404 when listing the global roles of an account that does not exist
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000/global-roles' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  Scenario: 205 - Should return 206 when the page does not hold all the global roles
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-205",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-205@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/GROUPS_ADMIN' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/GROUPS_AUDITOR' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/global-roles?size=1' with method 'GET'
    Then I expect status code is 206
    And  I expect '{{response.body.content.length}}' is '1'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles?size=1' with method 'GET'
    Then I expect status code is 206
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.totalElements}}' is '2'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  ###############################################################
  ######## Delete (DELETE /accounts/{accountId}/global-roles/{role}) ########
  ###############################################################

  Scenario: 301 - Should remove a global role from the given account and audit the delete
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-301",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-301@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ADMINISTRATORS_MANAGER' with method 'POST'
    Then I expect status code is 201
    And  I store 'globalRoleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/ADMINISTRATORS_MANAGER' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "SELECT operation, payload->'global_roles'->>'role' AS role FROM global_roles_audit WHERE gro_id = '{{ctx.globalRoleId}}' AND operation = 'DELETE'"
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].role}}' is 'ADMINISTRATORS_MANAGER'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should return 404 when the account does not hold the role
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-302",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-302@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/GROUPS_ADMIN' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.global_role.not_assigned'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 303 - Should return 404 when the account does not exist
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000/global-roles/GROUPS_ADMIN' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  Scenario: 304 - Should return 400 when the role is unknown
    When I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a003/global-roles/SUPER_ADMIN' with method 'DELETE'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.global_role.unknown'

  Scenario: 305 - Should remove the global roles of an account when the account is deleted
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gro-305",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gro-305@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/global-roles/APPLICATIONS_ADMIN' with method 'POST'
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "SELECT gro_id FROM global_roles WHERE act_id = '{{ctx.accountId}}'"
    Then  I expect 0 database results
