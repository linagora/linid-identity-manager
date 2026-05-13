Feature: Test API Organizational unit endpoints

  # Note: Background handles authentication before each Scenario
  #       and storing root organisation unit ID in context.
  # Scenario 101 overrides the Authorization header to test 401 behavior.

  ################## Authentication #######################
  ## 101 Should return 401 without valid authentication on organizational unit

  ################## Create (POST /organizational-units) ##############
  ## 201 Should create an organizational unit with valid data
  ## 202 Should return 400 with missing required fields
  ## 203 Should return 400 with root name/type
  ## 204 Should return 404 with unknown parent
  ## 205 Should return 400 with another organizational unit with same name and type

  ################## Find All (GET /organizational-units) #############
  ## 301 Should return paginated list of organizational-units

  ################## Find By Id (GET /organizational-units/{id}) ######
  ## 401 Should return 200 for existing organizational-unit
  ## 402 Should return 404 for unknown organizational-unit id

  ################## Delete (DELETE /organizational-units/{id}) #######
  ## 501 Should return 204 when deleting existing organizational-unit
  ## 502 Should return 400 when deleting root organizational-unit

  ################## Update (PUT /organizational-units/{id}) #######
  ## 601 Should return 200 update only name and type
  ## 602 Should return 400 when trying to set root as type or name
  ## 603 Should return 400 when trying to update root organizational unit
  ## 604 Should return 400 when trying to update another organizational unit with same name and type

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

  ####################################################
  ################## Authentication ##################
  ####################################################

  Scenario: 101 - Should return 401 without valid authentication on organizational unit
    Given I set http header 'Authorization' with 'Bearer badtoken'
    When  I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test",
        "type": "test"
      }
      """
    Then  I expect status code is 401

  ################################################################
  ################## Create (POST /organizational-units) #########
  ################################################################

  Scenario: 201 - Should create an organizational unit with valid data
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test",
        "type": "test"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body | dump}}' as 'json' to have length 7
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.name}}' is 'test'
    And  I expect '{{response.body.type}}' is 'test'
    And  I expect '{{response.body.createdBy}}' is not empty
    And  I expect '{{response.body.updatedBy}}' is not empty
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/organizational-units/{{response.body.id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 202 - Should return 400 with missing required field "<field>"
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": <parent>,
        "name": <name>,
        "type": <type>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    Examples:
      | field  | parent           | name   | type |
      | parent | null             | null   | null |
      | name   | "{{ctx.rootID}}" | null   | null |
      | type   | "{{ctx.rootID}}" | "test" | null |

  Scenario Outline: 203 - Should return 400 with root name/type
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "<name>",
        "type": "<type>"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is "The organizational unit name/type 'root' is reserved for system use."
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.root'
    And  I expect '{{response.body.status}}' is '400'

    Examples:
      | name | type |
      | root | root |
      | root | test |
      | test | root |

  Scenario: 204 - Should return 404 with unknown parent
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 201
    And  I store 'parentID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.parentID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.parentID}}",
        "name": "test2",
        "type": "test2"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.error}}' is 'Organizational unit not found: {{ctx.parentID}}'
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  Scenario: 205 - Should return 400 with another organizational unit with same name and type
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 201
    And  I store 'ou1ID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.already_exists'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou1ID}}' with method 'DELETE'
    Then I expect status code is 204

  #################################################################
  ################## Find All (GET /organizational-units) #########
  #################################################################

  Scenario: 301 - Should return paginated list of organizational-units
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 201
    And  I store 'ouId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units?name=lk_test*' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.ouId}}'
    And  I expect '{{response.body.content[0].name}}' is 'test1'
    And  I expect '{{response.body.content[0].type}}' is 'test1'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  #################################################################
  ################## Find By Id (GET /organizational-units/{id}) ##
  #################################################################

  Scenario: 401 - Should return 200 for existing organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test2"
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ouID}}'
    And  I expect '{{response.body.name}}' is 'test1'
    And  I expect '{{response.body.type}}' is 'test2'
    And  I expect '{{response.body.createdBy}}' is '00000000-0000-0000-0000-00000000a001'
    And  I expect '{{response.body.updatedBy}}' is '00000000-0000-0000-0000-00000000a001'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 402 - Should return 404 for unknown organizational unit id
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  ####################################################
  ################## Delete (DELETE /organizational-units/{id}) ###
  ####################################################

  Scenario: 501 - Should return 204 when deleting existing organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 404

  Scenario: 502 - Should return 400 when deleting root organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.rootID}}' with method 'DELETE'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.root.delete'

  #################################################################
  ################## Update (PUT /organizational-units/{id}) ######
  #################################################################

  Scenario: 601 - Should return 200 update only name and type
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test2"
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ouID}}'
    And  I expect '{{response.body.name}}' is 'test1'
    And  I expect '{{response.body.type}}' is 'test2'
    And  I expect '{{response.body.createdBy}}' is '00000000-0000-0000-0000-00000000a001'
    And  I expect '{{response.body.updatedBy}}' is '00000000-0000-0000-0000-00000000a001'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty
    And  I store 'lastInsertDate' as '{{response.body.insertDate}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test3",
        "type": "test4"
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ouID}}'
    And  I expect '{{response.body.name}}' is 'test3'
    And  I expect '{{response.body.type}}' is 'test4'
    And  I expect '{{response.body.createdBy}}' is '00000000-0000-0000-0000-00000000a001'
    And  I expect '{{response.body.updatedBy}}' is '00000000-0000-0000-0000-00000000a001'
    And  I expect '{{response.body.insertDate}}' is "{{ctx.lastInsertDate}}"
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 602 - Should return 400 when trying to set root as <field>
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test2"
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "<name>",
        "type": "<type>"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.root'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | field | name | type |
      | name  | root | test |
      | type  | test | root |

  Scenario: 603 - Should return 400 when trying to update another organizational unit with same name and type
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.rootID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test2"
      }
      """
    Then I expect status code is 400
    And  I store 'ouID' as '{{response.body.id}}' in context
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.root.update'

  Scenario: 604 - Should return 400 when trying to update root organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 201
    And  I store 'ou1_ID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test2",
        "type": "test2"
      }
      """
    Then I expect status code is 201
    And  I store 'ou2_ID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou2_ID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1"
      }
      """
    Then I expect status code is 400
    And  I store 'ouID' as '{{response.body.id}}' in context
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.already_exists'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou1_ID}}' with method 'DELETE'
    Then I expect status code is 204
