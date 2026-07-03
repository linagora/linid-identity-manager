Feature: Test API Application Rule endpoints

  # Note: Background handles authentication before each Scenario.
  # Endpoint authorization behavior is covered by a dedicated authorization feature.
  # Each scenario creates its own parent application and deletes it at the end;
  # deleting an application cascades to its rules.

  ################## Create (POST /applications/{id}/rules) ###########
  ## 101 Should create a rule always disabled and with a computed checksum
  ## 102 Should return 400 with a bad request payload (missing required fields)
  ## 103 Should return 400 when the code is already used within the application
  ## 104 Should allow the same code across different applications
  ## 105 Should return 404 when the application does not exist

  ################## Find All (GET /applications/{id}/rules) ##########
  ## 201 Should return the rules of an application sorted by priority ascending

  ################## Find By Id (GET /applications/{id}/rules/{ruleId})#
  ## 301 Should return 200 for an existing rule
  ## 302 Should return 404 for an unknown rule id
  ## 303 Should return 404 for a rule belonging to another application

  ################## Delete (DELETE /applications/{id}/rules/{ruleId}) #
  ## 401 Should return 204 when deleting an existing rule
  ## 402 Should return 404 when deleting an unknown rule

  ################## Update (PUT /applications/{id}/rules/{ruleId}) ####
  ## 501 Should update a rule and toggle its disabled state
  ## 502 Should return 404 when updating an unknown rule
  ## 503 Should return 400 when updating with a code used by another rule
  ## 504 Should return 400 with a bad request payload (missing required fields)

  ################## OPA policy generation ############################
  ## 601 Should regenerate the application policy when a rule is toggled active
  ## 602 Should regenerate the application policy without the fragment when a rule is deleted

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
  ################## Create ###########################
  ####################################################

  Scenario: 101 - Should create a rule always disabled and with a computed checksum
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-101",
        "name": "Application Rule 101",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "description": "First rule",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.applicationId}}' is '{{ctx.appId}}'
    And  I expect '{{response.body.code}}' is 'RULE_1'
    And  I expect '{{response.body.description}}' is 'First rule'
    And  I expect '{{response.body.priority}}' is '1'
    And  I expect '{{response.body.script}}' is 'return false;'
    And  I expect '{{response.body.disabled}}' is 'true'
    And  I expect '{{response.body.createdBy}}' is not empty
    And  I expect '{{response.body.updatedBy}}' is not empty
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 102 - Should return 400 creating a rule with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-102-<field>",
        "name": "Application Rule 102",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": <code>,
        "priority": <priority>,
        "script": <script>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | field    | code     | priority | script          |
      | code     | null     | 1        | "return false;" |
      | priority | "RULE_1" | null     | "return false;" |
      | script   | "RULE_1" | 1        | null            |

  Scenario: 103 - Should return 400 when the code is already used within the application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-103",
        "name": "Application Rule 103",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 2,
        "script": "return true;",
        "disabled": false
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application_rule.code.already_exists'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 104 - Should allow the same code across different applications
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-104-a",
        "name": "Application Rule 104 A",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appAId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-104-b",
        "name": "Application Rule 104 B",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appBId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appAId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appBId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appAId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appBId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 105 - Should return 404 when the application does not exist
    When I request '{{env.E2E_API_URL}}/applications/00000000-0000-0000-0000-000000000000/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  ####################################################
  ################## Find All #########################
  ####################################################

  Scenario: 201 - Should return the rules of an application sorted by priority ascending
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-201",
        "name": "Application Rule 201",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_3",
        "priority": 3,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_2",
        "priority": 2,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '3'
    And  I expect '{{response.body.content[0].code}}' is 'RULE_1'
    And  I expect '{{response.body.content[0].priority}}' is '1'
    And  I expect '{{response.body.content[1].code}}' is 'RULE_2'
    And  I expect '{{response.body.content[1].priority}}' is '2'
    And  I expect '{{response.body.content[2].code}}' is 'RULE_3'
    And  I expect '{{response.body.content[2].priority}}' is '3'
    And  I expect '{{response.body.content[0].createdBy}}' is 'admin_fn admin_ln'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Find By Id #######################
  ####################################################

  Scenario: 301 - Should return 200 for an existing rule
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-301",
        "name": "Application Rule 301",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ruleId}}'
    And  I expect '{{response.body.applicationId}}' is '{{ctx.appId}}'
    And  I expect '{{response.body.code}}' is 'RULE_1'
    And  I expect '{{response.body.priority}}' is '1'
    And  I expect '{{response.body.disabled}}' is 'true'
    And  I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should return 404 for an unknown rule id
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-302",
        "name": "Application Rule 302",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/00000000-0000-0000-0000-000000000000' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application_rule.not_found'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 303 - Should return 404 for a rule belonging to another application
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-303-a",
        "name": "Application Rule 303 A",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appAId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-303-b",
        "name": "Application Rule 303 B",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appBId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appAId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appBId}}/rules/{{ctx.ruleId}}' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application_rule.not_found'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appAId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appBId}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Delete ###########################
  ####################################################

  Scenario: 401 - Should return 204 when deleting an existing rule
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-401",
        "name": "Application Rule 401",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'GET'
    Then I expect status code is 404

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 402 - Should return 404 when deleting an unknown rule
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-402",
        "name": "Application Rule 402",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/00000000-0000-0000-0000-000000000000' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application_rule.not_found'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Update ###########################
  ####################################################

  Scenario: 501 - Should update a rule and toggle its disabled state
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-501",
        "name": "Application Rule 501",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "description": "First rule",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1_UPDATED",
        "description": "First rule updated",
        "priority": 5,
        "script": "return true;",
        "disabled": false
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.code}}' is 'RULE_1_UPDATED'
    And  I expect '{{response.body.description}}' is 'First rule updated'
    And  I expect '{{response.body.priority}}' is '5'
    And  I expect '{{response.body.script}}' is 'return true;'
    And  I expect '{{response.body.disabled}}' is 'false'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 502 - Should return 404 when updating an unknown rule
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-502",
        "name": "Application Rule 502",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/00000000-0000-0000-0000-000000000000' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application_rule.not_found'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 503 - Should return 400 when updating with a code used by another rule
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-503",
        "name": "Application Rule 503",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_2",
        "priority": 2,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 2,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.application_rule.code.already_exists'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 504 - Should return 400 updating a rule with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-504-<field>",
        "name": "Application Rule 504",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "return false;",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": <code>,
        "priority": <priority>,
        "script": <script>,
        "disabled": <disabled>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | field    | code     | priority | script          | disabled |
      | code     | null     | 1        | "return false;" | false    |
      | priority | "RULE_1" | null     | "return false;" | false    |
      | script   | "RULE_1" | 1        | null            | false    |
      | disabled | "RULE_1" | 1        | "return false;" | null     |

  ####################################################
  ################## OPA policy generation ############
  ####################################################

  Scenario: 601 - Should regenerate the application policy when a rule is toggled active
    Given I setup database with driver "postgres" host "{{env.DATABASE_HOST}}" port 5432 user "{{env.DATABASE_ADMIN_USER}}" password "{{env.DATABASE_ADMIN_PASSWORD}}" database "{{env.DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-601",
        "name": "Application Rule 601",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.admin == true",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    # Rules are created disabled: while the rule stays disabled its fragment must NOT be in the policy.
    When I execute sql request "SELECT script FROM applications WHERE code = $1" with values:
      """
      ["app-rule-601"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].script}}' not contains 'input.user.admin == true'

    # Enabling the rule must regenerate the policy with its fragment and reset the deployment date.
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.admin == true",
        "disabled": false
      }
      """
    Then I expect status code is 200

    When I execute sql request "SELECT script, script_checksum, deployed_at FROM applications WHERE code = $1" with values:
      """
      ["app-rule-601"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].script}}' contains 'package authz["app-rule-601"]'
    And  I expect '{{ctx.dbResults[0].script}}' contains 'signals contains "allow" if input.user.admin == true'
    And  I expect '{{ctx.dbResults[0].script_checksum}}' is not empty
    And  I expect '{{ctx.dbResults[0].deployed_at}}' is empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 602 - Should regenerate the application policy without the fragment when a rule is deleted
    Given I setup database with driver "postgres" host "{{env.DATABASE_HOST}}" port 5432 user "{{env.DATABASE_ADMIN_USER}}" password "{{env.DATABASE_ADMIN_PASSWORD}}" database "{{env.DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-rule-602",
        "name": "Application Rule 602",
        "type": "OIDC",
        "claimsTemplate": "{}"
      }
      """
    Then I expect status code is 201
    And  I store 'appId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules' with method 'POST' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.admin == true",
        "disabled": false
      }
      """
    Then I expect status code is 201
    And  I store 'ruleId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'PUT' with body:
      """
      {
        "code": "RULE_1",
        "priority": 1,
        "script": "signals contains \"allow\" if input.user.admin == true",
        "disabled": false
      }
      """
    Then I expect status code is 200

    When I execute sql request "SELECT script FROM applications WHERE code = $1" with values:
      """
      ["app-rule-602"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].script}}' contains 'input.user.admin == true'

    # Deleting the rule must regenerate the policy without its fragment.
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}/rules/{{ctx.ruleId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT script, deployed_at FROM applications WHERE code = $1" with values:
      """
      ["app-rule-602"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].script}}' not contains 'input.user.admin == true'
    And  I expect '{{ctx.dbResults[0].script}}' contains 'package authz["app-rule-602"]'
    And  I expect '{{ctx.dbResults[0].deployed_at}}' is empty

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.appId}}' with method 'DELETE'
    Then I expect status code is 204
