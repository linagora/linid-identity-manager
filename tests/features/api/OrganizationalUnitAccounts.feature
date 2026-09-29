Feature: Test API Organizational unit account endpoints

  # Note: Background handles authentication before each Scenario.
  # Each scenario creates its own organizational unit, account and functional role, and deletes them at the end;
  # deleting an account or an organizational unit cascades to their relationships.
  # Creating an account automatically attaches it to its organizational unit.
  # Attaching an account through the endpoint requires a functional role; a role held by an account cannot be
  # deleted until the relationship is detached.

  ################## Attach (POST /organizational-units/{id}/accounts) ##################
  ## 101 Should attach an account to an organizational unit
  ## 102 Should return 400 when the account is already attached
  ## 103 Should return 404 when attaching to an unknown organizational unit
  ## 104 Should return 404 when attaching an unknown account
  ## 105 Should return 400 with a bad request payload (missing accountId)
  ## 106 Should default the relationship extra parameters when the attach payload omits them
  ## 107 Should return 400 with a bad request payload (missing roleId)
  ## 108 Should return 404 when attaching with an unknown role
  ## 109 Should refuse to delete the functional role while the relationship exists

  ################## Update relationship (PUT /organizational-units/{id}/accounts/{accountId}) ##################
  ## 201 Should update the relationship extra parameters
  ## 202 Should return 404 when the account is not attached
  ## 203 Should return 404 when updating on an unknown organizational unit
  ## 204 Should keep the relationship extra parameters when the update payload omits them
  ## 205 Should update the functional role of the relationship
  ## 206 Should return 404 when updating with an unknown role

  ################## Detach (DELETE /organizational-units/{id}/accounts/{accountId}) ##################
  ## 301 Should detach an account from an organizational unit
  ## 302 Should return 404 when the account is not attached
  ## 303 Should return 404 when detaching from an unknown organizational unit

  Background:
    Given I set http header 'Authorization' with '{{ env.E2E_AUTH_TOKEN }}'
    And   I set http header 'Content-Type' with 'application/x-www-form-urlencoded'
    When  I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=admin&password=password&scope=openid email profile roles
      """
    Then  I expect status code is 200
    And   I store 'accessToken' as '{{response.body.access_token}}' in context
    And   I set http header 'Authorization' with 'Bearer {{ctx.accessToken}}'
    And   I set http header 'Content-Type' with 'application/json'

    When I request '{{env.E2E_API_URL}}/organizational-units?name=root&type=root' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{ response.body.content.length}}' is "1"
    And  I store 'rootID' as '{{response.body.content[0].id}}' in context

  #########################################################################
  ################## Attach (POST /organizational-units/{id}/accounts) ####
  #########################################################################

  Scenario: 101 - Should attach an account to an organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-101",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-101",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-101@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootID}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-OUA-101",
        "name": "Role 101",
        "description": "Role created by scenario 101",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'roleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "roleId": "{{ctx.roleId}}",
        "extraParameters": {
          "role": "member"
        }
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.organizationalUnitId}}' is '{{ctx.ouId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.roleId}}' is '{{ctx.roleId}}'
    And  I expect '{{response.body.extraParameters.role}}' is 'member'
    And  I expect '{{response.body.createdBy}}' is not empty
    And  I expect '{{response.body.updatedBy}}' is not empty

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts?id={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.content[0].roleId}}' is '{{ctx.roleId}}'
    And  I expect '{{response.body.content[0].roleName}}' is 'Role 101'
    And  I expect '{{response.body.content[0].relationExtraParameters.role}}' is 'member'
    And  I expect '{{response.body.content[0].createdBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.content[0].insertDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 102 - Should return 400 when the account is already attached
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-102",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-102",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-102@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.ouId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-OUA-102",
        "name": "Role 102",
        "description": "Role created by scenario 102",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'roleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "roleId": "{{ctx.roleId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.account.already_attached'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 103 - Should return 404 when attaching to an unknown organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-4000-8000-000000000000/accounts' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-4000-8000-000000000000",
        "roleId": "00000000-0000-4000-8000-000000000000",
        "extraParameters": {}
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  Scenario: 104 - Should return 404 when attaching an unknown account
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-104",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-4000-8000-000000000000",
        "roleId": "00000000-0000-4000-8000-000000000000",
        "extraParameters": {}
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 105 - Should return 400 with a bad request payload (missing accountId)
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-105",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "roleId": "00000000-0000-4000-8000-000000000000",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.validation'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 106 - Should default the relationship extra parameters when the attach payload omits them
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-106",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-106",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-106@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootID}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-OUA-106",
        "name": "Role 106",
        "description": "Role created by scenario 106",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'roleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "roleId": "{{ctx.roleId}}"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.roleId}}' is '{{ctx.roleId}}'
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 107 - Should return 400 with a bad request payload (missing roleId)
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-107",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-4000-8000-000000000000",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.validation'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 108 - Should return 404 when attaching with an unknown role
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-108",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-108",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-108@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootID}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "roleId": "00000000-0000-4000-8000-000000000000",
        "extraParameters": {}
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.role.not_found'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts?id={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '0'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 109 - Should refuse to delete the functional role while the relationship exists
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-109",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-109",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-109@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootID}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-OUA-109",
        "name": "Role 109",
        "description": "Role created by scenario 109",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'roleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "roleId": "{{ctx.roleId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.roleId}}' is '{{ctx.roleId}}'

    When I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.role.in_use'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts?id={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.content[0].roleId}}' is '{{ctx.roleId}}'
    And  I expect '{{response.body.content[0].roleName}}' is 'Role 109'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  ###########################################################################################
  ################## Update relationship (PUT /organizational-units/{id}/accounts/{accountId})
  ###########################################################################################

  Scenario: 201 - Should update the relationship extra parameters
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-201",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-201",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-201@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.ouId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'PUT' with body:
      """
      {
        "extraParameters": {
          "role": "manager"
        }
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.organizationalUnitId}}' is '{{ctx.ouId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.extraParameters.role}}' is 'manager'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts?id={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].relationExtraParameters.role}}' is 'manager'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 202 - Should return 404 when the account is not attached
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-202",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-202",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-202@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootID}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'PUT' with body:
      """
      {
        "extraParameters": {}
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.account.not_attached'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 203 - Should return 404 when updating on an unknown organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-4000-8000-000000000000/accounts/00000000-0000-4000-8000-000000000000' with method 'PUT' with body:
      """
      {
        "extraParameters": {}
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  Scenario: 204 - Should keep the relationship extra parameters when the update payload omits them
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-204",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-204",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-204@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.ouId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'PUT' with body:
      """
      {
        "extraParameters": {
          "role": "manager"
        }
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'PUT' with body:
      """
      {}
      """
    Then I expect status code is 200
    And  I expect '{{response.body.extraParameters.role}}' is 'manager'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 205 - Should update the functional role of the relationship
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-205",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-205",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-205@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.ouId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-OUA-205",
        "name": "Role 205",
        "description": "Role created by scenario 205",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'roleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'PUT' with body:
      """
      {
        "roleId": "{{ctx.roleId}}"
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.organizationalUnitId}}' is '{{ctx.ouId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.roleId}}' is '{{ctx.roleId}}'
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts?id={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].roleId}}' is '{{ctx.roleId}}'
    And  I expect '{{response.body.content[0].roleName}}' is 'Role 205'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/organizational-units' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.ouId}}'
    And  I expect '{{response.body.content[0].roleId}}' is '{{ctx.roleId}}'
    And  I expect '{{response.body.content[0].roleName}}' is 'Role 205'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 206 - Should return 404 when updating with an unknown role
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-206",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-206",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-206@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.ouId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'PUT' with body:
      """
      {
        "roleId": "00000000-0000-4000-8000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.role.not_found'
    And  I expect '{{response.body.status}}' is '404'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts?id={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content[0].roleId}}' is '00000000-0000-4000-8000-00000000f001'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  ##############################################################################################
  ################## Detach (DELETE /organizational-units/{id}/accounts/{accountId}) ###########
  ##############################################################################################

  Scenario: 301 - Should detach an account from an organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-301",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-301",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-301@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.ouId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts?id={{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '0'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should return 404 when the account is not attached
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-account-302",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-302",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oua-302@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootID}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.account.not_attached'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 303 - Should return 404 when detaching from an unknown organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-4000-8000-000000000000/accounts/00000000-0000-4000-8000-000000000000' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'
