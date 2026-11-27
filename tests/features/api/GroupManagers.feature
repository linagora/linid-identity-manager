Feature: Test API Group manager endpoints

  # Note: Background logs in three users and stores the root organizational unit ID in context.
  # - admin creates and deletes the fixtures (groups, organizational units, accounts) and names the managers of
  #   organizational units;
  # - user1, an account without any global role (it only holds the MANAGER and MEMBER functional roles), calls every
  #   endpoint under test (managers endpoints, group creation): these endpoints do not restrict access, every
  #   authenticated user can call them;
  # - norole, an account without any role, global or functional, calls each endpoint in 106, 206 and 308.
  # The default Authorization header is the admin one; scenarios switch to user1 or norole before each call under test.
  # Seeded accounts: user1 (00000000-0000-4000-8000-00000000a002), user2 (00000000-0000-4000-8000-00000000a003) and
  # norole (00000000-0000-4000-8000-00000000a013).
  # Deleting a group or an account cascades to its managers.
  # Groups created by admin have no organizational unit: their creator never becomes a manager.

  ################## Add (POST /groups/{id}/managers) ##################
  ## 101 Should add a manager to a group
  ## 102 Should return 400 when the account is already a manager of the group
  ## 103 Should return 404 when adding a manager to an unknown group
  ## 104 Should return 404 when adding an unknown account as a manager of a group
  ## 105 Should return 400 with a bad request payload (missing accountId when adding a manager to a group)
  ## 106 Should let an account without any role add a manager to a group
  ## 107 Should keep the group manager foreign key names that the service maps to a 404

  ################## Find (GET /groups/{id}/managers) ##################
  ## 201 Should return the managers of the group with their identity and the audit
  ## 202 Should return 206 when more managers of the group remain on the next pages
  ## 203 Should filter the managers of the group
  ## 204 Should not return the managers of another group
  ## 205 Should return 404 when listing the managers of an unknown group
  ## 206 Should let an account without any role list the managers of a group

  ################## Remove (DELETE /groups/{id}/managers/{accountId}) ##################
  ## 301 Should remove the last manager of a group
  ## 302 Should let a manager remove themselves from a group
  ## 303 Should return 404 when the account is not a manager of the group
  ## 304 Should return 404 when removing a manager from an unknown group
  ## 305 Should remove the managers when the account or the group is deleted
  ## 306 Should audit the addition and the removal of a group manager
  ## 307 Should audit the managers removed with a deleted account or group
  ## 308 Should let an account without any role remove a manager of a group

  ################## Creator (POST /groups by an organizational unit manager) ##################
  ## 401 Should make the manager of the organizational unit a manager of the group they create
  ## 402 Should make the manager of a parent organizational unit a manager of the group they create in a child unit
  ## 403 Should not make the creator a manager when they manage neither the organizational unit nor its parents

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
    When  I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=norole&password=password&scope=openid email profile roles
      """
    Then  I expect status code is 200
    And   I store 'noRoleToken' as '{{response.body.access_token}}' in context
    And   I store 'user1Id' as '00000000-0000-4000-8000-00000000a002' in context
    And   I store 'user2Id' as '00000000-0000-4000-8000-00000000a003' in context
    And   I store 'noRoleId' as '00000000-0000-4000-8000-00000000a013' in context
    And   I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    And   I set http header 'Content-Type' with 'application/json'

    When I request '{{env.E2E_API_URL}}/organizational-units?name=root&type=root' with method 'GET'
    Then I expect status code is 200
    And  I store 'rootId' as '{{response.body.content[0].id}}' in context

  ######################################################################
  ################## Add (POST /groups/{id}/managers) ##################
  ######################################################################

  Scenario: 101 - Should add a manager to a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-101",
        "name": "Group manager 101",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.groupId}}' is '{{ctx.groupId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.user2Id}}'
    And  I expect '{{response.body.createdBy}}' is '{{ctx.user1Id}}'
    And  I expect '{{response.body.updatedBy}}' is '{{ctx.user1Id}}'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 102 - Should return 400 when the account is already a manager of the group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-102",
        "name": "Group manager 102",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.manager.already_exists'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 103 - Should return 404 when adding a manager to an unknown group
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 104 - Should return 404 when adding an unknown account as a manager of a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-104",
        "name": "Group manager 104",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-0000-0000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 105 - Should return 400 with a bad request payload (missing accountId when adding a manager to a group)
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-105",
        "name": "Group manager 105",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.validation'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 106 - Should let an account without any role add a manager to a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-106",
        "name": "Group manager 106",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.noRoleToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.accountId}}' is '{{ctx.user2Id}}'
    And  I expect '{{response.body.createdBy}}' is '{{ctx.noRoleId}}'
    And  I expect '{{response.body.updatedBy}}' is '{{ctx.noRoleId}}'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 107 - Should keep the group manager foreign key names that the service maps to a 404
    # When a concurrent request deletes the group or the account between the checks and the insert, the service maps
    # the violated foreign key to the matching 404 by its name. PostgreSQL generates these names from the columns:
    # renaming a column or naming the constraint changes them, and the service would then return a 500.
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I execute sql request "SELECT conname FROM pg_constraint WHERE conrelid = 'group_managers'::regclass AND contype = 'f' ORDER BY conname"
    Then I expect 2 database results
    And  I expect '{{ctx.dbResults[0].conname}}' is 'group_managers_act_id_fkey'
    And  I expect '{{ctx.dbResults[1].conname}}' is 'group_managers_grp_id_fkey'

  ######################################################################
  ################## Find (GET /groups/{id}/managers) ##################
  ######################################################################

  Scenario: 201 - Should return the managers of the group with their identity and the audit
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-201",
        "name": "Group manager 201",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'managerId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
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
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 202 - Should return 206 when more managers of the group remain on the next pages
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-202",
        "name": "Group manager 202",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers?size=1&sort=lastname,asc' with method 'GET'
    Then I expect status code is 206
    And  I expect '{{response.body.totalElements}}' is '2'
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].lastname}}' is 'user1_ln'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers?size=2' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '2'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 203 - Should filter the managers of the group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-203",
        "name": "Group manager 203",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers?lastname=user2_ln' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers?accountId={{ctx.user1Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].lastname}}' is 'user1_ln'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 204 - Should not return the managers of another group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-204-a",
        "name": "Group manager 204 A",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupAId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-204-b",
        "name": "Group manager 204 B",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupBId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupBId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupAId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupAId}}/managers?groupId={{ctx.groupBId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupAId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupBId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 205 - Should return 404 when listing the managers of an unknown group
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000/managers' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 206 - Should let an account without any role list the managers of a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-206",
        "name": "Group manager 206",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    Given I set http header 'Authorization' with 'Bearer {{ctx.noRoleToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  ##################################################################################
  ################## Remove (DELETE /groups/{id}/managers/{accountId}) #############
  ##################################################################################

  Scenario: 301 - Should remove the last manager of a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-301",
        "name": "Group manager 301",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should let a manager remove themselves from a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-302",
        "name": "Group manager 302",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers/{{ctx.user1Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 303 - Should return 404 when the account is not a manager of the group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-303",
        "name": "Group manager 303",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.manager.not_found'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 304 - Should return 404 when removing a manager from an unknown group
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 305 - Should remove the managers when the account or the group is deleted
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-305",
        "name": "Group manager 305",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-grm-305",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-grm-305@example.com",
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
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    # Deleting the account removes it from the managers of the group.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    # Deleting the group removes all its managers.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT grm_id FROM group_managers WHERE grp_id = $1" with values:
      """
      [
        "{{ctx.groupId}}"
      ]
      """
    Then I expect 0 database results

  Scenario: 306 - Should audit the addition and the removal of a group manager
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-306",
        "name": "Group manager 306",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'managerId' as '{{response.body.id}}' in context

    When I execute sql request "SELECT operation, payload->'group'->>'code' AS group_code, payload->'account'->>'external_id' AS account, payload->'changed_by'->>'external_id' AS changed_by FROM group_managers_audit WHERE grm_id = $1" with values:
      """
      [
        "{{ctx.managerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].operation}}' is 'INSERT'
    And  I expect '{{ctx.dbResults[0].group_code}}' is 'grp-manager-306'
    And  I expect '{{ctx.dbResults[0].account}}' is 'user2'
    And  I expect '{{ctx.dbResults[0].changed_by}}' is 'user1'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT operation FROM group_managers_audit WHERE grm_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.managerId}}"
      ]
      """
    Then I expect 1 database results

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 307 - Should audit the managers removed with a deleted account or group
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-307",
        "name": "Group manager 307",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-grm-307",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-grm-307@example.com",
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
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'accountManagerId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'user2ManagerId' as '{{response.body.id}}' in context

    # Deleting the account writes the DELETE audit row of its manager record.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT payload->'group_managers'->>'grp_id' AS grp_id, payload->'group_managers'->>'act_id' AS act_id FROM group_managers_audit WHERE grm_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.accountManagerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].grp_id}}' is '{{ctx.groupId}}'
    And  I expect '{{ctx.dbResults[0].act_id}}' is '{{ctx.accountId}}'

    # Deleting the group writes the DELETE audit row of each of its manager records.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT payload->'group_managers'->>'grp_id' AS grp_id, payload->'group_managers'->>'act_id' AS act_id FROM group_managers_audit WHERE grm_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.user2ManagerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].grp_id}}' is '{{ctx.groupId}}'
    And  I expect '{{ctx.dbResults[0].act_id}}' is '{{ctx.user2Id}}'

  Scenario: 308 - Should let an account without any role remove a manager of a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-308",
        "name": "Group manager 308",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    Given I set http header 'Authorization' with 'Bearer {{ctx.noRoleToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  ##############################################################################
  ################## Creator (POST /groups by an organizational unit manager) ##
  ##############################################################################

  Scenario: 401 - Should make the manager of the organizational unit a manager of the group they create
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "grp-manager-401",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-401",
        "name": "Group manager 401",
        "organizationalUnitId": "{{ctx.ouId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user1Id}}'
    And  I expect '{{response.body.content[0].createdBy}}' is 'user1_fn user1_ln'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 402 - Should make the manager of a parent organizational unit a manager of the group they create in a child unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "grp-manager-402-parent",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'parentOuId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.parentOuId}}",
        "name": "grp-manager-402-child",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'childOuId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.parentOuId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-402",
        "name": "Group manager 402",
        "organizationalUnitId": "{{ctx.childOuId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user1Id}}'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.childOuId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.parentOuId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 403 - Should not make the creator a manager when they manage neither the organizational unit nor its parents
    # user1 manages the child unit only, and user2 manages the parent unit: neither makes user1 a manager of a
    # group of the parent unit, nor of a group without organizational unit.
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootId}}",
        "name": "grp-manager-403-parent",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'parentOuId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.parentOuId}}",
        "name": "grp-manager-403-child",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'childOuId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.childOuId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.parentOuId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-403-parent",
        "name": "Group manager 403 parent",
        "organizationalUnitId": "{{ctx.parentOuId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-manager-403-none",
        "name": "Group manager 403 without organizational unit",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupWithoutOuId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupWithoutOuId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupWithoutOuId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.childOuId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.parentOuId}}' with method 'DELETE'
    Then I expect status code is 204
