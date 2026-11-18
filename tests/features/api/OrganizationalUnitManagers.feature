Feature: Test API Organizational unit manager endpoints

  # Note: Background logs in two users and stores the root organizational unit ID in context.
  # - admin creates and deletes the fixtures (organizational units, accounts);
  # - user1, an account without any role, calls every managers endpoint under test: this ticket does not
  #   restrict access to these endpoints.
  # The default Authorization header is the admin one; scenarios switch to user1 before each call under test.
  # Seeded accounts: user1 (00000000-0000-4000-8000-00000000a002) and user2 (00000000-0000-4000-8000-00000000a003).
  # Deleting an organizational unit or an account cascades to its managers.

  ################## Add (POST /organizational-units/{id}/managers) ##################
  ## 101 Should add a manager to an organizational unit
  ## 102 Should return 400 when the account is already a manager of the organizational unit
  ## 103 Should return 404 when adding a manager to an unknown organizational unit
  ## 104 Should return 404 when adding an unknown account as a manager
  ## 105 Should return 400 with a bad request payload (missing accountId when adding a manager)

  ################## Find (GET /organizational-units/{id}/managers) ##################
  ## 201 Should return the managers of the organizational unit with their identity and the audit
  ## 202 Should return 206 when more managers remain on the next pages
  ## 203 Should filter the managers of the organizational unit
  ## 204 Should not return the managers of another organizational unit
  ## 205 Should return 404 when listing the managers of an unknown organizational unit

  ################## Remove (DELETE /organizational-units/{id}/managers/{accountId}) ##################
  ## 301 Should remove the last manager of an organizational unit
  ## 302 Should let a manager remove themselves
  ## 303 Should return 404 when the account is not a manager of the organizational unit
  ## 304 Should return 404 when removing a manager from an unknown organizational unit
  ## 305 Should remove the managers when the account or the organizational unit is deleted
  ## 306 Should audit the addition and the removal of a manager
  ## 307 Should keep the deleted account or organizational unit in the audit of the managers removed with it

  Background:
    Given I set http header 'Authorization' with '{{ env.E2E_AUTH_TOKEN }}'
    And   I set http header 'Content-Type' with 'application/x-www-form-urlencoded'
    When  I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=admin&password=password&scope=openid email profile roles
      """
    Then  I expect status code is 200
    And   I store 'adminToken' as '{{response.body.access_token}}' in context
    When  I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=user1&password=password&scope=openid email profile roles
      """
    Then  I expect status code is 200
    And   I store 'userToken' as '{{response.body.access_token}}' in context
    And   I store 'user1Id' as '00000000-0000-4000-8000-00000000a002' in context
    And   I store 'user2Id' as '00000000-0000-4000-8000-00000000a003' in context
    And   I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    And   I set http header 'Content-Type' with 'application/json'

    When I request '{{env.E2E_API_URL}}/organizational-units?name=root&type=root' with method 'GET'
    Then I expect status code is 200
    And  I store 'rootId' as '{{response.body.content[0].id}}' in context

  ######################################################################
  ################## Add (POST /organizational-units/{id}/managers) ####
  ######################################################################

  Scenario: 101 - Should add a manager to an organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-101",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.organizationalUnitId}}' is '{{ctx.ouId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.user2Id}}'
    And  I expect '{{response.body.createdBy}}' is '{{ctx.user1Id}}'
    And  I expect '{{response.body.updatedBy}}' is '{{ctx.user1Id}}'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 102 - Should return 400 when the account is already a manager of the organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-102",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.manager.already_exists'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 103 - Should return 404 when adding a manager to an unknown organizational unit
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-0000-0000-000000000000/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  Scenario: 104 - Should return 404 when adding an unknown account as a manager
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-104",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-0000-0000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 105 - Should return 400 with a bad request payload (missing accountId when adding a manager)
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-105",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.validation'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  ######################################################################
  ################## Find (GET /organizational-units/{id}/managers) ####
  ######################################################################

  Scenario: 201 - Should return the managers of the organizational unit with their identity and the audit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-201",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'managerId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.managerId}}'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'
    And  I expect '{{response.body.content[0].lastname}}' is 'user2_ln'
    And  I expect '{{response.body.content[0].firstname}}' is 'user2_fn'
    And  I expect '{{response.body.content[0].email}}' is 'user2@example.com'
    And  I expect '{{response.body.content[0].createdBy}}' is 'user1_fn user1_ln'
    And  I expect '{{response.body.content[0].updatedBy}}' is 'user1_fn user1_ln'
    And  I expect '{{response.body.content[0].insertDate}}' is not empty
    And  I expect '{{response.body.content[0].updateDate}}' is not empty

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 202 - Should return 206 when more managers remain on the next pages
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-202",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers?size=1&sort=lastname,asc' with method 'GET'
    Then I expect status code is 206
    And  I expect '{{response.body.totalElements}}' is '2'
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].lastname}}' is 'user1_ln'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers?size=2' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '2'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 203 - Should filter the managers of the organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-203",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers?lastname=user2_ln' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers?accountId={{ctx.user1Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].lastname}}' is 'user1_ln'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 204 - Should not return the managers of another organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-204-a",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouAId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-204-b",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouBId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouBId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouAId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouAId}}/managers?organizationalUnitId={{ctx.ouBId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouAId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouBId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 205 - Should return 404 when listing the managers of an unknown organizational unit
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-0000-0000-000000000000/managers' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  ##################################################################################
  ################## Remove (DELETE /organizational-units/{id}/managers/{accountId}) #
  ##################################################################################

  Scenario: 301 - Should remove the last manager of an organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-301",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should let a manager remove themselves
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-302",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers/{{ctx.user1Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 303 - Should return 404 when the account is not a manager of the organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-303",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.manager.not_found'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 304 - Should return 404 when removing a manager from an unknown organizational unit
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-0000-0000-000000000000/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  Scenario: 305 - Should remove the managers when the account or the organizational unit is deleted
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-305",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oum-305",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oum-305@example.com",
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

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    # Deleting the account removes it from the managers of the organizational unit.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    # Deleting the organizational unit removes all its managers.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT oum_id FROM organizational_unit_managers WHERE oun_id = $1" with values:
      """
      [
        "{{ctx.ouId}}"
      ]
      """
    Then I expect 0 database results

  Scenario: 306 - Should audit the addition and the removal of a manager
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-306",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'managerId' as '{{response.body.id}}' in context

    When I execute sql request "SELECT operation, payload->'organizational_unit'->>'name' AS ou_name, payload->'account'->>'external_id' AS account, payload->'changed_by'->>'external_id' AS changed_by FROM organizational_unit_managers_audit WHERE oum_id = $1" with values:
      """
      [
        "{{ctx.managerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].operation}}' is 'INSERT'
    And  I expect '{{ctx.dbResults[0].ou_name}}' is 'ou-manager-306'
    And  I expect '{{ctx.dbResults[0].account}}' is 'user2'
    And  I expect '{{ctx.dbResults[0].changed_by}}' is 'user1'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT operation FROM organizational_unit_managers_audit WHERE oum_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.managerId}}"
      ]
      """
    Then I expect 1 database results

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 307 - Should keep the deleted account or organizational unit in the audit of the managers removed with it
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "ou-manager-307",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oum-307",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-oum-307@example.com",
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

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'accountManagerId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'user2ManagerId' as '{{response.body.id}}' in context

    # The account is already deleted when the audit of its manager record is written.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT payload->'organizational_unit'->>'name' AS ou_name, payload->'account'->>'external_id' AS account FROM organizational_unit_managers_audit WHERE oum_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.accountManagerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ou_name}}' is 'ou-manager-307'
    And  I expect '{{ctx.dbResults[0].account}}' is 'ext-oum-307'

    # The organizational unit is already deleted when the audit of its manager records is written.
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT payload->'organizational_unit'->>'name' AS ou_name, payload->'account'->>'external_id' AS account FROM organizational_unit_managers_audit WHERE oum_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.user2ManagerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ou_name}}' is 'ou-manager-307'
    And  I expect '{{ctx.dbResults[0].account}}' is 'user2'
