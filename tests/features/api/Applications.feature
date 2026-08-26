Feature: Test API Application endpoints

  # Note: Background handles authentication before each Scenario.
  # Endpoint authorization behavior is covered by a dedicated authorization feature.

  ################## Create (POST /applications) #####################
  ## 101 Should create an application with valid data
  ## 102 Should return 400 with a bad request payload (missing required fields)
  ## 103 Should return 400 with another application with same code
  ## 104 Should return 400 with invalid application code

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

  ################## Deploy (POST /applications/{id}/deploy) ##########
  ## 601 Should trigger deployment of an application no force
  ## 602 Should trigger deployment of an application force true
  ## 603 Should return 404 when deploying an unknown application

  ################## Export Script (GET /applications/{id}/script) #####
  ## 701 Should export application Rego script
  ## 702 Should return 404 when application script is empty
  ## 703 Should return 404 when application does not exist

  ################## System application (code LINID) #################
  ## 801 Should expose the seeded LINID system application
  ## 802 Should return 400 when updating the LINID system application
  ## 803 Should return 400 when deleting the LINID system application

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
        "claimsTemplate": "{ \"sub\": \"id\" }",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.code}}' is 'app-101'
    And  I expect '{{response.body.name}}' is 'Application 101'
    And  I expect '{{response.body.description}}' is 'An application for tests'
    And  I expect '{{response.body.type}}' is 'OIDC'
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'
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
        "claimsTemplate": <claimsTemplate>,
        "extraParameters": {}
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": "{}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application.code.already_exists'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app103Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 104 - Should return 400 with invalid application code
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app/104",
        "name": "Application 104",
        "type": "OIDC",
        "claimsTemplate": "{}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
    And  I expect '{{response.body.content[0].extraParameters | dump}}' is '{}'
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": "{ \"sub\": \"id\" }",
        "extraParameters": { "test": "test" }
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
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": "{}",
        "extraParameters": {}
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
        "claimsTemplate": <claimsTemplate>,
        "extraParameters": {}
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
  ################## Deploy (POST /applications/{id}/deploy) ##########
  ####################################################

  Scenario: 601 - Should trigger deployment of an application no force
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-601-no-force",
        "name": "Application 601",
        "type": "OIDC",
        "claimsTemplate": "{}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'app601Id' as '{{response.body.id}}' in context

    # Create a rule
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app601Id}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_ADMIN_601",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.organizationalUnit == \"public\"",
        "disabled": false
      }
      """application/json
    Then I expect status code is 201

    # Deploy the application (this will publish the generated script to OPA)
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app601Id}}/deploy' with method 'POST'
    Then I expect status code is 200
    And  I expect '{{response.body.deployedAt}}' is not empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app601Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 602 - Should trigger deployment of an application force true
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-602-force",
        "name": "Application 602",
        "type": "OIDC",
        "claimsTemplate": "{}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'app602Id' as '{{response.body.id}}' in context

    # Create a rule
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app602Id}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_ADMIN_602",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.organizationalUnit == \"public\"",
        "disabled": false
      }
      """
    Then I expect status code is 201

    # Deploy the application for the first time
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app602Id}}/deploy' with method 'POST'
    Then I expect status code is 200
    And  I expect '{{response.body.deployedAt}}' is not empty

    # Redeploy with force=true to force redeployment
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app602Id}}/deploy?force=true' with method 'POST'
    Then I expect status code is 200
    And  I expect '{{response.body.deployedAt}}' is not empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app602Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 603 - Should return 404 when deploying an unknown application
    When I request '{{env.E2E_API_URL}}/applications/00010000-0000-0000-0000-000000000000/deploy' with method 'POST'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  ############################################################################
  ################## Export Script (GET /applications/{id}/script) ###########
  ############################################################################
  Scenario: 701 - Should export application Rego script
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-701",
        "name": "Application 701",
        "type": "OIDC",
        "claimsTemplate": "{}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'app701Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app701Id}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_ADMIN_701",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.admin == true",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'rule701Id' as '{{response.body.id}}' in context

    # Regenerate policy
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app701Id}}/rules/{{ctx.rule701Id}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_ADMIN_701",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.admin == true",
        "disabled": false
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app701Id}}/script' with method 'GET'
    Then I expect status code is 200
    And  I expect http header "Content-Type" contains "text/plain"
    And  I expect http header "Content-Disposition" contains "attachment; filename=\"app-701.rego\""
    And  I expect "{{response.body}}" contains "package authz[\"app-701\"]"
    And  I expect "{{response.body}}" contains "input.user.admin == true"

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app701Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 702 - Should return 404 when application script is empty
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-702",
        "name": "Application 702",
        "type": "OIDC",
        "claimsTemplate": "{}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'app702Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app702Id}}/script' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.script.not_found'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.app702Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 703 - Should return 404 when application does not exist
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000/script' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  ####################################################
  ################## System application (LINID) ######
  ####################################################

  Scenario: 801 - Should expose the seeded LINID system application
    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].name}}' is 'LINID - Identity Manager'
    And  I expect '{{response.body.content[0].description}}' is 'System identity manager application'
    And  I expect '{{response.body.content[0].type}}' is 'System'

  Scenario: 802 - Should return 400 when updating the LINID system application
    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I store 'linidId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.linidId}}' with method 'PUT' with body:
      """
      {
        "code": "LINID",
        "name": "Renamed system application",
        "type": "OIDC",
        "claimsTemplate": "{}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application.system_reserved'

    # The application must be left untouched.
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.linidId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.name}}' is 'LINID - Identity Manager'

  Scenario: 803 - Should return 400 when deleting the LINID system application
    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I store 'linidId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.linidId}}' with method 'DELETE'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application.system_reserved'

    # The application must still exist.
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.linidId}}' with method 'GET'
    Then I expect status code is 200
