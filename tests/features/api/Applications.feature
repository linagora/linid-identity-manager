Feature: Test API Application endpoints

  # Note: Background handles authentication before each Scenario.
  # Endpoint authorization behavior is covered by a dedicated authorization feature.

  ################## Create (POST /applications) #####################
  ## 101 Should create an application with valid data
  ## 102 Should return 400 with a bad request payload (missing required fields)
  ## 103 Should return 400 with another application with same code

  ################## Find All (GET /applications) ####################
  ## 201 Should return paginated list of applications

  ################## Find By Id (GET /applications/{id}) #############
  ## 301 Should return 200 for existing application
  ## 302 Should return 404 for unknown application id

  ################## Delete (DELETE /applications/{id}) ##############
  ## 401 Should return 204 when deleting existing application
  ## 402 Should return 404 when deleting unknown application

  ################## Update (PUT /applications/{id}) #################
  ## 501 Should return 200 updating an application
  ## 502 Should return 404 when updating an unknown application
  ## 503 Should return 400 when updating with a code used by another application
  ## 504 Should return 400 with a bad request payload (missing required fields)

  ################## Roles (GET/PUT /applications/{id}/roles) ########
  ## 601 Should update the roles of an application
  ## 602 Should return 404 when updating roles of an unknown application
  ## 603 Should return 400 with an invalid roles payload (object / invalid JSON / invalid role)
  ## 604 Should retrieve the roles of an application
  ## 605 Should return 404 when retrieving roles of an unknown application

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
  ################## Create (POST /applications) #####
  ####################################################

  Scenario: 101 - Should create an application with valid data
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-101",
        "name": "Application 101",
        "description": "An application for tests",
        "type": "OIDC",
        "claimsTemplate": "{ \"sub\": \"id\" }"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.code}}' is 'app-101'
    And  I expect '{{response.body.name}}' is 'Application 101'
    And  I expect '{{response.body.description}}' is 'An application for tests'
    And  I expect '{{response.body.type}}' is 'OIDC'
    And  I expect '{{response.body.createdBy}}' is not empty
    And  I expect '{{response.body.updatedBy}}' is not empty
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/applications/{{response.body.id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 102 - Should return 400 with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": <code>,
        "name": <name>,
        "type": <type>,
        "claimsTemplate": <claimsTemplate>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    Examples:
      | field          | code      | name   | type   | claimsTemplate |
      | code           | null      | "name" | "OIDC" | "{}"           |
      | name           | "app-102" | null   | "OIDC" | "{}"           |
      | type           | "app-102" | "name" | null   | "{}"           |
      | claimsTemplate | "app-102" | "name" | "OIDC" | null           |

  Scenario: 103 - Should return 400 with another application with same code
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-103",
        "name": "Application 103",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app103Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-103",
        "name": "Another Application 103",
        "type": "SAML",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application.code.already_exists'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app103Id}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Find All (GET /applications) ####
  ####################################################

  Scenario: 201 - Should return paginated list of applications
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-201",
        "name": "Application 201",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app201Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications?code=app-201' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.app201Id}}'
    And  I expect '{{response.body.content[0].code}}' is 'app-201'
    And  I expect '{{response.body.content[0].name}}' is 'Application 201'
    And  I expect '{{response.body.content[0].type}}' is 'OIDC'
    And  I expect '{{response.body.content[0].createdBy}}' is 'admin_fn admin_ln'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app201Id}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Find By Id (GET /applications/{id}) #############
  ####################################################

  Scenario: 301 - Should return 200 for existing application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-301",
        "name": "Application 301",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app301Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app301Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.app301Id}}'
    And  I expect '{{response.body.code}}' is 'app-301'
    And  I expect '{{response.body.name}}' is 'Application 301'
    And  I expect '{{response.body.type}}' is 'OIDC'
    And  I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app301Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should return 404 for unknown application id
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  ####################################################
  ################## Delete (DELETE /applications/{id}) ##############
  ####################################################

  Scenario: 401 - Should return 204 when deleting existing application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-401",
        "name": "Application 401",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{response.body.id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 402 - Should return 404 when deleting unknown application
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  ####################################################
  ################## Update (PUT /applications/{id}) #################
  ####################################################

  Scenario: 501 - Should return 200 updating an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-501",
        "name": "Application 501",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app501Id' as '{{response.body.id}}' in context
    And  I store 'lastInsertDate' as '{{response.body.insertDate}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app501Id}}' with method 'PUT' with body:
      """
      {
        "code": "app-501-updated",
        "name": "Application 501 updated",
        "description": "Updated description",
        "type": "SAML",
        "claimsTemplate": "{ \"sub\": \"id\" }"
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app501Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.app501Id}}'
    And  I expect '{{response.body.code}}' is 'app-501-updated'
    And  I expect '{{response.body.name}}' is 'Application 501 updated'
    And  I expect '{{response.body.description}}' is 'Updated description'
    And  I expect '{{response.body.type}}' is 'SAML'
    And  I expect '{{response.body.insertDate}}' is "{{ctx.lastInsertDate}}"
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app501Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 502 - Should return 404 when updating an unknown application
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000' with method 'PUT' with body:
      """
      {
        "code": "app-502",
        "name": "Application 502",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  Scenario: 503 - Should return 400 when updating with a code used by another application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-503-a",
        "name": "Application 503 A",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app503aId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-503-b",
        "name": "Application 503 B",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app503bId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app503bId}}' with method 'PUT' with body:
      """
      {
        "code": "app-503-a",
        "name": "Application 503 B",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application.code.already_exists'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app503aId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app503bId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 504 - Should return 400 with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-504-<field>",
        "name": "Application 504",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app504Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app504Id}}' with method 'PUT' with body:
      """
      {
        "code": <code>,
        "name": <name>,
        "type": <type>,
        "claimsTemplate": <claimsTemplate>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app504Id}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | field          | code      | name   | type   | claimsTemplate |
      | code           | null      | "name" | "OIDC" | "{}"           |
      | name           | "app-504" | null   | "OIDC" | "{}"           |
      | type           | "app-504" | "name" | null   | "{}"           |
      | claimsTemplate | "app-504" | "name" | "OIDC" | null           |

  ####################################################
  ################## Roles (GET/PUT /applications/{id}/roles) ########
  ####################################################

  Scenario: 601 - Should update the roles of an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-601",
        "name": "Application 601",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app601Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app601Id}}/roles' with method 'PUT' with body:
      """
      [
        { "name": "admin", "description": "Grants full administrative access" },
        { "name": "user" }
      ]
      """
    Then I expect status code is 200
    And  I expect '{{response.body.length}}' is "2"
    And  I expect '{{response.body[0].name}}' is 'admin'
    And  I expect '{{response.body[0].description}}' is 'Grants full administrative access'
    And  I expect '{{response.body[1].name}}' is 'user'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app601Id}}/roles' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.length}}' is "2"
    And  I expect '{{response.body[0].name}}' is 'admin'
    And  I expect '{{response.body[0].description}}' is 'Grants full administrative access'
    And  I expect '{{response.body[1].name}}' is 'user'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app601Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 602 - Should return 404 when updating roles of an unknown application
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000/roles' with method 'PUT' with body:
      """
      [{ "name": "admin" }]
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  Scenario Outline: 603 - Should return 400 with an invalid roles payload (<case>)
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "<code>",
        "name": "Application 603",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app603Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app603Id}}/roles' with method 'PUT' with body:
      """
      <roles>
      """
    Then I expect status code is 400

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app603Id}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | case                       | code          | roles                  |
      | object instead of array    | app-603-obj   | {}                     |
      | invalid JSON               | app-603-json  | [                      |
      | role without name          | app-603-name  | [{ "description": "x" }] |
      | role with blank name       | app-603-blank | [{ "name": "  " }]     |

  Scenario: 604 - Should retrieve the roles of an application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-604",
        "name": "Application 604",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'app604Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app604Id}}/roles' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.length}}' is "0"

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app604Id}}/roles' with method 'PUT' with body:
      """
      [{ "name": "auditor", "description": "Read-only access" }]
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app604Id}}/roles' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.length}}' is "1"
    And  I expect '{{response.body[0].name}}' is 'auditor'
    And  I expect '{{response.body[0].description}}' is 'Read-only access'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app604Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 605 - Should return 404 when retrieving roles of an unknown application
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000/roles' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'
