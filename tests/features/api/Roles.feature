Feature: Test API Role endpoints

  ################## Create (POST /roles) ##################
  ## 101 Should create a role with valid data
  ## 102 Should return 400 with missing required role fields
  ## 103 Should return 400 with duplicate role code

  ################## Find All (GET /roles) #################
  ## 201 Should return paginated list of roles
  ## 202 Should filter roles by code

  ################## Find By Id (GET /roles/{id}) ##########
  ## 301 Should return 200 for existing role
  ## 302 Should return 404 for unknown role id

  ################## Delete (DELETE /roles/{id}) ###########
  ## 401 Should return 204 when deleting existing role
  ## 402 Should return 404 when deleting unknown role

  ################## Update (PUT /roles/{id}) ##############
  ## 501 Should update the editable attributes of an existing role
  ## 502 Should update an existing role without extra-parameters
  ## 503 Should return 404 when updating an unknown role
  ## 504 Should return 400 when updating a role with a duplicate code
  ## 505 Should return 400 when updating role with invalid fields

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

  ####################################################
  ################## Create (POST /roles) #############
  ####################################################

  Scenario Outline: 101 - Should create a role with valid data
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-101",
        "name": "Test Role",
        "description": "Role created by scenario 101",
        "extraParameters": <extraParameters>
      }
      """
    Then  I expect status code is 201
    And   I expect '{{response.body | dump}}' as 'json' to have length 9
    And   I expect '{{response.body.id}}' is not empty
    And   I expect '{{response.body.code}}' is 'ROLE-101'
    And   I expect '{{response.body.name}}' is 'Test Role'
    And   I expect '{{response.body.description}}' is 'Role created by scenario 101'
    And   I expect '{{response.body.createdBy}}' is not empty
    And   I expect '{{response.body.updatedBy}}' is not empty
    And   I expect '{{response.body.insertDate}}' is not empty
    And   I expect '{{response.body.updateDate}}' is not empty
    And   I expect '{{response.body.extraParameters | dump}}' is '<result>'

    When  I request '{{env.E2E_API_URL}}/roles/{{response.body.id}}' with method 'DELETE'
    Then  I expect status code is 204

    Examples:
      | extraParameters | result |
      | null            | {}     |
      | {}              | {}     |

  Scenario Outline: 102 - Should return 400 with invalid role fields
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": <code>,
        "name": <name>,
        "description": <description>,
        "extraParameters": {}
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.error}}' is '<error>'
    And   I expect '{{response.body.errorKey}}' is '<errorKey>'
    And   I expect '{{response.body.status}}' is '400'

    Examples:
      | code         | name | description | error             | errorKey         |
      | ""           | ""   | ""          | Validation failed | error.validation |
      | "bad/test"   | ""   | ""          | Validation failed | error.validation |

  Scenario: 103 - Should return 400 with duplicate role code
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-103",
        "name": "Test Role",
        "description": "Role created by scenario 103",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'roleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-103",
        "name": "Test Role 2",
        "description": "Duplicated Role created by scenario 103",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.error}}' is 'Functional role already exists for code \'ROLE-103\''
    And   I expect '{{response.body.errorKey}}' is 'error.role.code.already_exists'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then  I expect status code is 204

  ####################################################
  ################## Find All (GET /roles) ############
  ####################################################

  Scenario: 201 - Should return paginated list of roles
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-201",
        "name": "Find All Role",
        "description": "Role created by scenario 201",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'roleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles?code=ROLE-201' with method 'GET'
    Then  I expect status code is 200
    And   I expect '{{response.body.totalElements}}' is '1'
    And   I expect '{{response.body.content[0].id}}' is '{{ctx.roleId}}'
    And   I expect '{{response.body.content[0].code}}' is 'ROLE-201'
    And   I expect '{{response.body.content[0].name}}' is 'Find All Role'
    And   I expect '{{response.body.content[0].description}}' is 'Role created by scenario 201'
    And   I expect '{{response.body.content[0].createdBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.content[0].updatedBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.content[0].extraParameters | dump}}' is '{}'
    And   I expect '{{response.body.content[0].insertDate}}' is not empty
    And   I expect '{{response.body.content[0].updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 202 - Should filter roles by code
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-202-A",
        "name": "First Filter Role",
        "description": "Role that should not match",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'firstRoleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-202-B",
        "name": "Second Filter Role",
        "description": "Role that should match",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'secondRoleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles?code=ROLE-202-B' with method 'GET'
    Then  I expect status code is 200
    And   I expect '{{response.body.totalElements}}' is '1'
    And   I expect '{{response.body.content.length}}' is '1'
    And   I expect '{{response.body.content[0].id}}' is '{{ctx.secondRoleId}}'
    And   I expect '{{response.body.content[0].code}}' is 'ROLE-202-B'
    And   I expect '{{response.body.content[0].name}}' is 'Second Filter Role'

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.firstRoleId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.secondRoleId}}' with method 'DELETE'
    Then  I expect status code is 204

  ####################################################
  ################## Find By Id (GET /roles/{id}) #####
  ####################################################

  Scenario: 301 - Should return 200 for existing role
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-301",
        "name": "Find By Id Role",
        "description": "Role created by scenario 301",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'roleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'GET'
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.roleId}}'
    And   I expect '{{response.body.code}}' is 'ROLE-301'
    And   I expect '{{response.body.name}}' is 'Find By Id Role'
    And   I expect '{{response.body.description}}' is 'Role created by scenario 301'
    And   I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.extraParameters | dump}}' is '{}'
    And   I expect '{{response.body.insertDate}}' is not empty
    And   I expect '{{response.body.updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 302 - Should return 404 for unknown role id
    When  I request '{{env.E2E_API_URL}}/roles/00000000-0000-4000-0000-000000000000' with method 'GET'
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.role.not_found'
    And   I expect '{{response.body.status}}' is '404'

  ####################################################
  ################## Delete (DELETE /roles/{id}) ######
  ####################################################

  Scenario: 401 - Should return 204 when deleting existing role
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-401",
        "name": "Delete Role",
        "description": "Role created by scenario 401",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'roleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'GET'
    Then  I expect status code is 404

  Scenario: 402 - Should return 404 when deleting unknown role
    When  I request '{{env.E2E_API_URL}}/roles/00000000-0000-4000-0000-000000000000' with method 'DELETE'
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.role.not_found'
    And   I expect '{{response.body.status}}' is '404'

  ####################################################
  ################## Update (PUT /roles/{id}) #########
  ####################################################

  Scenario: 501 - Should update the editable attributes of an existing role
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-501",
        "name": "Before Update",
        "description": "Original role description",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'roleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'PUT' with body:
      """
      {
        "code": "ROLE-501-UPDATED",
        "name": "After Update",
        "description": "Updated role description",
        "extraParameters": {
          "updated": true
        }
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.roleId}}'
    And   I expect '{{response.body.code}}' is 'ROLE-501-UPDATED'
    And   I expect '{{response.body.name}}' is 'After Update'
    And   I expect '{{response.body.description}}' is 'Updated role description'
    And   I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.extraParameters | dump}}' is '{"updated":true}'
    And   I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.insertDate}}' is not empty
    And   I expect '{{response.body.updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 502 - Should update an existing role without extra-parameters
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-502",
        "name": "Before Update",
        "description": "Original role description",
        "extraParameters": {"test":"test"}
      }
      """
    Then  I expect status code is 201
    And   I store 'roleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'PUT' with body:
      """
      {
        "code": "ROLE-502-UPDATED",
        "name": "After Update",
        "description": "Updated role description",
        "extraParameters": null
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.roleId}}'
    And   I expect '{{response.body.code}}' is 'ROLE-502-UPDATED'
    And   I expect '{{response.body.name}}' is 'After Update'
    And   I expect '{{response.body.description}}' is 'Updated role description'
    And   I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'
    And   I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.insertDate}}' is not empty
    And   I expect '{{response.body.updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 503 - Should return 404 when updating an unknown role
    When  I request '{{env.E2E_API_URL}}/roles/00000000-0000-4000-0000-000000000000' with method 'PUT' with body:
      """
      {
        "code": "ROLE-504",
        "name": "Unknown Role",
        "description": "This role does not exist",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.role.not_found'
    And   I expect '{{response.body.status}}' is '404'

  Scenario: 504 - Should return 400 when updating a role with a duplicate code
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-504-A",
        "name": "First Role",
        "description": "First role created by scenario 504",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'firstRoleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-504-B",
        "name": "Second Role",
        "description": "Second role created by scenario 504",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'secondRoleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.secondRoleId}}' with method 'PUT' with body:
      """
      {
        "code": "ROLE-504-A",
        "name": "Updated Second Role",
        "description": "Trying to use an existing code",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.error}}' is 'Functional role already exists for code \'ROLE-504-A\''
    And   I expect '{{response.body.errorKey}}' is 'error.role.code.already_exists'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.firstRoleId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.secondRoleId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario Outline: 505 - Should return 400 when updating role with invalid fields
    When  I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "ROLE-505",
        "name": "First Role",
        "description": "First role created by scenario 505",
        "extraParameters": {}
      }
      """
    Then  I expect status code is 201
    And   I store 'roleId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'PUT' with body:
      """
      {
        "code": <code>,
        "name": <name>,
        "description": <description>,
        "extraParameters": {}
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.error}}' is '<error>'
    And   I expect '{{response.body.errorKey}}' is '<errorKey>'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then  I expect status code is 204

    Examples:
      | code         | name | description | error             | errorKey         |
      | ""           | ""   | ""          | Validation failed | error.validation |
      | "bad/test"   | ""   | ""          | Validation failed | error.validation |
