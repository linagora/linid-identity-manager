Feature: Test API Application manager endpoints

  # Note: Background logs in three users and stores the root organizational unit ID in context (used to create accounts).
  # - admin creates and deletes the fixtures (applications, accounts);
  # - user1, an account without any global role (it only holds the MANAGER and MEMBER functional roles), calls every
  #   managers endpoint under test: these endpoints do not restrict access, every authenticated user can call them;
  # - norole, an account without any role, global or functional, calls each endpoint in 106, 206 and 308.
  # The default Authorization header is the admin one; scenarios switch to user1 or norole before each call under test.
  # Seeded accounts: user1 (00000000-0000-4000-8000-00000000a002), user2 (00000000-0000-4000-8000-00000000a003) and
  # norole (00000000-0000-4000-8000-00000000a013).
  # Deleting an application or an account cascades to its managers.

  ################## Add (POST /applications/{id}/managers) ##################
  ## 101 Should add a manager to an application
  ## 102 Should return 400 when the account is already a manager of the application
  ## 103 Should return 404 when adding a manager to an unknown application
  ## 104 Should return 404 when adding an unknown account as a manager of an application
  ## 105 Should return 400 with a bad request payload (missing accountId when adding a manager to an application)
  ## 106 Should let an account without any role add a manager to an application
  ## 107 Should keep the foreign key names that the service maps to a 404

  ################## Find (GET /applications/{id}/managers) ##################
  ## 201 Should return the managers of the application with their identity and the audit
  ## 202 Should return 206 when more managers of the application remain on the next pages
  ## 203 Should filter the managers of the application
  ## 204 Should not return the managers of another application
  ## 205 Should return 404 when listing the managers of an unknown application
  ## 206 Should let an account without any role list the managers of an application

  ################## Remove (DELETE /applications/{id}/managers/{accountId}) ##################
  ## 301 Should remove the last manager of an application
  ## 302 Should let a manager remove themselves from an application
  ## 303 Should return 404 when the account is not a manager of the application
  ## 304 Should return 404 when removing a manager from an unknown application
  ## 305 Should remove the managers when the account or the application is deleted
  ## 306 Should audit the addition and the removal of an application manager
  ## 307 Should audit the managers removed with a deleted account or application
  ## 308 Should let an account without any role remove a manager of an application

  ################## System application (code LINID) ##################
  ## 401 Should add a manager to and remove a manager from the LINID application

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
  ################## Add (POST /applications/{id}/managers) ############
  ######################################################################

  Scenario: 101 - Should add a manager to an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-101",
        "name": "Application Manager 101",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.applicationId}}' is '{{ctx.appId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.user2Id}}'
    And  I expect '{{response.body.createdBy}}' is '{{ctx.user1Id}}'
    And  I expect '{{response.body.updatedBy}}' is '{{ctx.user1Id}}'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 102 - Should return 400 when the account is already a manager of the application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-102",
        "name": "Application Manager 102",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application_manager.already_exists'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 103 - Should return 404 when adding a manager to an unknown application
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  Scenario: 104 - Should return 404 when adding an unknown account as a manager of an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-104",
        "name": "Application Manager 104",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-0000-0000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 105 - Should return 400 with a bad request payload (missing accountId when adding a manager to an application)
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-105",
        "name": "Application Manager 105",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.validation'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 106 - Should let an account without any role add a manager to an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-106",
        "name": "Application Manager 106",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.noRoleToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
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
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 107 - Should keep the foreign key names that the service maps to a 404
    # When a concurrent request deletes the application or the account between the checks and the insert, the service
    # maps the violated foreign key to the matching 404 by its name. PostgreSQL generates these names from the columns:
    # renaming a column or naming the constraint changes them, and the service would then return a 500.
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I execute sql request "SELECT conname FROM pg_constraint WHERE conrelid = 'application_managers'::regclass AND contype = 'f' ORDER BY conname"
    Then I expect 2 database results
    And  I expect '{{ctx.dbResults[0].conname}}' is 'application_managers_act_id_fkey'
    And  I expect '{{ctx.dbResults[1].conname}}' is 'application_managers_app_id_fkey'

  ######################################################################
  ################## Find (GET /applications/{id}/managers) ############
  ######################################################################

  Scenario: 201 - Should return the managers of the application with their identity and the audit
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-201",
        "name": "Application Manager 201",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'managerId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'GET'
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
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 202 - Should return 206 when more managers of the application remain on the next pages
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-202",
        "name": "Application Manager 202",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers?size=1&sort=lastname,asc' with method 'GET'
    Then I expect status code is 206
    And  I expect '{{response.body.totalElements}}' is '2'
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].lastname}}' is 'user1_ln'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers?size=2' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '2'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 203 - Should filter the managers of the application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-203",
        "name": "Application Manager 203",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers?lastname=user2_ln' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers?accountId={{ctx.user1Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].lastname}}' is 'user1_ln'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 204 - Should not return the managers of another application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-204-a",
        "name": "Application Manager 204-a",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appAId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-204-b",
        "name": "Application Manager 204-b",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appBId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appBId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appAId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appAId}}/managers?applicationId={{ctx.appBId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appAId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appBId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 205 - Should return 404 when listing the managers of an unknown application
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000/managers' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  Scenario: 206 - Should let an account without any role list the managers of an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-206",
        "name": "Application Manager 206",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    Given I set http header 'Authorization' with 'Bearer {{ctx.noRoleToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  ##################################################################################
  ################## Remove (DELETE /applications/{id}/managers/{accountId}) #######
  ##################################################################################

  Scenario: 301 - Should remove the last manager of an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-301",
        "name": "Application Manager 301",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should let a manager remove themselves from an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-302",
        "name": "Application Manager 302",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user1Id}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers/{{ctx.user1Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 303 - Should return 404 when the account is not a manager of the application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-303",
        "name": "Application Manager 303",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application_manager.not_found'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 304 - Should return 404 when removing a manager from an unknown application
    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  Scenario: 305 - Should remove the managers when the account or the application is deleted
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-305",
        "name": "Application Manager 305",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-apm-305",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-apm-305@example.com",
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
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    # Deleting the account removes it from the managers of the application.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].accountId}}' is '{{ctx.user2Id}}'

    # Deleting the application removes all its managers.
    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT apm_id FROM application_managers WHERE app_id = $1" with values:
      """
      [
        "{{ctx.appId}}"
      ]
      """
    Then I expect 0 database results

  Scenario: 306 - Should audit the addition and the removal of an application manager
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-306",
        "name": "Application Manager 306",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I store 'managerId' as '{{response.body.id}}' in context

    When I execute sql request "SELECT operation, payload->'application'->>'code' AS app_code, payload->'account'->>'external_id' AS account, payload->'changed_by'->>'external_id' AS changed_by FROM application_managers_audit WHERE apm_id = $1" with values:
      """
      [
        "{{ctx.managerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].operation}}' is 'INSERT'
    And  I expect '{{ctx.dbResults[0].app_code}}' is 'app-manager-306'
    And  I expect '{{ctx.dbResults[0].account}}' is 'user2'
    And  I expect '{{ctx.dbResults[0].changed_by}}' is 'user1'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT operation FROM application_managers_audit WHERE apm_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.managerId}}"
      ]
      """
    Then I expect 1 database results

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 307 - Should audit the managers removed with a deleted account or application
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-307",
        "name": "Application Manager 307",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-apm-307",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-apm-307@example.com",
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
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'accountManagerId' as '{{response.body.id}}' in context
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
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

    When I execute sql request "SELECT payload->'application_managers'->>'app_id' AS app_id, payload->'application_managers'->>'act_id' AS act_id FROM application_managers_audit WHERE apm_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.accountManagerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].app_id}}' is '{{ctx.appId}}'
    And  I expect '{{ctx.dbResults[0].act_id}}' is '{{ctx.accountId}}'

    # Deleting the application writes the DELETE audit row of each of its manager records.
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT payload->'application_managers'->>'app_id' AS app_id, payload->'application_managers'->>'act_id' AS act_id FROM application_managers_audit WHERE apm_id = $1 AND operation = 'DELETE'" with values:
      """
      [
        "{{ctx.user2ManagerId}}"
      ]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].app_id}}' is '{{ctx.appId}}'
    And  I expect '{{ctx.dbResults[0].act_id}}' is '{{ctx.user2Id}}'

  Scenario: 308 - Should let an account without any role remove a manager of an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-manager-308",
        "name": "Application Manager 308",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201

    Given I set http header 'Authorization' with 'Bearer {{ctx.noRoleToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    Given I set http header 'Authorization' with 'Bearer {{ctx.adminToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  ######################################################################
  ################## System application (LINID) #######################
  ######################################################################

  Scenario: 401 - Should add a manager to and remove a manager from the LINID application
    # SystemApplicationValidator protects the LINID row and its roles, not its managers: each organization designates
    # a person who administers LinID, so managing LINID is not restricted.
    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I store 'linidId' as '{{response.body.content[0].id}}' in context

    Given I set http header 'Authorization' with 'Bearer {{ctx.userToken}}'
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.linidId}}/managers' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.user2Id}}"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.applicationId}}' is '{{ctx.linidId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.user2Id}}'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.linidId}}/managers/{{ctx.user2Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.linidId}}/managers' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'
