Feature: Test API Organizational unit endpoints

  # Note: Background handles authentication before each Scenario
  #       and storing root organisation unit ID in context.
  # Scenario 101 overrides the Authorization header to test 401 behavior.

  ################## Authentication #######################
  ## 101 Should return 401 without valid authentication on organizational unit

  ################## Create (POST /organizational-units) ##############
  ## 201 Should create an organizational unit with valid data
  ## 202 Should return 400 with missing required field "<field>"
  ## 203 Should return 400 with root name/type
  ## 204 Should return 404 with unknown parent
  ## 205 Should return 400 with another organizational unit with same name and type
  ## 206 Should create an organizational unit with empty extra parameters when they are omitted

  ################## Find All (GET /organizational-units) #############
  ## 301 Should return paginated list of organizational-units
  ## 302 Should filter organizational units by functional role

  ################## Find By Id (GET /organizational-units/{id}) ######
  ## 401 Should return 200 for existing organizational unit
  ## 402 Should return 404 for unknown organizational unit id

  ################## Delete (DELETE /organizational-units/{id}) #######
  ## 501 Should return 204 when deleting existing organizational unit
  ## 502 Should return 400 when deleting root organizational unit

  ################## Update (PUT /organizational-units/{id}) #######
  ## 601 Should return 200 update only name and type
  ## 602 Should return 400 when trying to set root as <field>
  ## 603 Should return 400 when trying to update root organizational unit
  ## 604 Should return 400 when trying to update another organizational unit with same name and type
  ## 605 Should keep the extra parameters when the update payload omits them

  ################## Update status (PUT /organizational-units/{id}/status) #######
  ## 701 Should auto-create a non-suspended status when the organizational unit is created
  ## 702 Should set a future suspension period and expose it in retrieval endpoints
  ## 703 Should accept an open-ended (permanent) future suspension
  ## 704 Should return 400 when suspensionPeriod is missing
  ## 705 Should return 400 when the suspension period start is after its end
  ## 706 Should return 400 when the suspension period start is in the past
  ## 707 Should return 400 when the suspension period end is in the past
  ## 708 Should return 404 when updating the status of an unknown organizational unit

  ################## Reactivate (PUT /organizational-units/{id}/status/reactivate) #######
  ## 709 Should reactivate a suspended organizational unit with a justification comment
  ## 710 Should return 400 when reactivating an organizational unit that is not suspended
  ## 711 Should return 404 when reactivating an unknown organizational unit

  ################## Find accounts of O.U. (GET /organizational-units/{id}/accounts) #######
  ## 801 Should return <user> for <ou> organizational-unit

  ################## Hierarchy closure (organizational_unit_closures) #######
  ## 901 Should remove the ancestor paths of a detached organizational unit and of its descendants
  ## 902 Should keep the ancestor paths reached through another parent when a relation is detached
  ## 903 Should keep the closure table consistent with the relations after successive deletions

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
        "type": "test",
        "extraParameters": {}
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
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body | dump}}' as 'json' to have length 8
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.name}}' is 'test'
    And  I expect '{{response.body.type}}' is 'test'
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'
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
        "type": <type>,
        "extraParameters": {}
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
        "type": "<type>",
        "extraParameters": {}
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
        "type": "test1",
        "extraParameters": {}
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
        "type": "test2",
        "extraParameters": {}
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
        "type": "test1",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ou1ID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.already_exists'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou1ID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 206 - Should create an organizational unit with empty extra parameters when they are omitted
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-206",
        "type": "test"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'
    And  I store 'ou206Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou206Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou206Id}}' with method 'DELETE'
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
    And  I expect '{{response.body.content[0].extraParameters | dump}}' is '{}'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should filter organizational units by functional role
    When I request '{{env.E2E_API_URL}}/organizational-units?roleNames=lk_*Viewer*' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '00000000-0000-4000-8000-00000000000e'
    And  I expect '{{response.body.content[0].name}}' is 'Division B1'
    And  I expect '{{response.body.content[0].roleNames}}' is 'Viewer'

    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-4000-8000-00000000000e' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.roleNames}}' is 'Viewer'

  #################################################################
  ################## Find By Id (GET /organizational-units/{id}) ##
  #################################################################

  Scenario: 401 - Should return 200 for existing organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test2",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ouID}}'
    And  I expect '{{response.body.name}}' is 'test1'
    And  I expect '{{response.body.type}}' is 'test2'
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'
    And  I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty
    And  I expect '{{response.body.parents.length}}' is "1"
    And  I expect '{{response.body.parents[0].id}}' is not empty
    And  I expect '{{response.body.parents[0].parent}}' is "{{ctx.rootID}}"
    And  I expect '{{response.body.parents[0].extraParameters | dump}}' is 'null'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 402 - Should return 404 for unknown organizational unit id
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1",
        "extraParameters": {}
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
        "type": "test1",
        "extraParameters": {}
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
        "type": "test2",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ouID}}'
    And  I expect '{{response.body.name}}' is 'test1'
    And  I expect '{{response.body.type}}' is 'test2'
    And  I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty
    And  I store 'lastInsertDate' as '{{response.body.insertDate}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test3",
        "type": "test4",
        "extraParameters": { "test": "test"}
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ouID}}'
    And  I expect '{{response.body.name}}' is 'test3'
    And  I expect '{{response.body.type}}' is 'test4'
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'
    And  I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
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
        "type": "test2",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "<name>",
        "type": "<type>",
        "extraParameters": {}
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

  Scenario: 603 - Should return 400 when trying to update root organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.rootID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test2",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I store 'ouID' as '{{response.body.id}}' in context
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.root.update'

  Scenario: 604 - Should return 400 when trying to update another organizational unit with same name and type
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ou1_ID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test2",
        "type": "test2",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ou2_ID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou2_ID}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "test1",
        "type": "test1",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I store 'ouID' as '{{response.body.id}}' in context
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.already_exists'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou1_ID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou2_ID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 605 - Should keep the extra parameters when the update payload omits them
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-605",
        "type": "test",
        "extraParameters": { "test": "test" }
      }
      """
    Then I expect status code is 201
    And  I store 'ou605Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou605Id}}' with method 'PUT' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "ou-605-updated",
        "type": "test"
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou605Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ou605Id}}' with method 'DELETE'
    Then I expect status code is 204

  #################################################################
  ################## Update status (PUT /organizational-units/{id}/status) #######
  #################################################################

  Scenario: 701 - Should auto-create a non-suspended status when the organizational unit is created
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test1",
        "type": "status-test1",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.suspensionPeriod}}' is empty
    And  I expect '{{response.body.statusReason}}' is empty
    And  I expect '{{response.body.isSuspended}}' is "false"
    And  I expect '{{response.body.status}}' is 'ACTIVE'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 702 - Should set a future suspension period and expose it in retrieval endpoints
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test2",
        "type": "status-test2",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2998-01-01T00:00:00Z",
          "end": "2999-01-01T00:00:00Z"
        },
        "reason": "REORGANIZATION",
        "subreason": "MERGER",
        "comment": "Suspended pending department merger"
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.suspensionPeriod.start}}' is not empty
    And  I expect '{{response.body.suspensionPeriod.end}}' is not empty
    And  I expect '{{response.body.suspensionReason}}' is 'REORGANIZATION'
    And  I expect '{{response.body.suspensionSubreason}}' is 'MERGER'
    And  I expect '{{response.body.suspensionComment}}' is 'Suspended pending department merger'
    And  I expect '{{response.body.isSuspended}}' is "false"

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.suspensionPeriod.start}}' is not empty
    And  I expect '{{response.body.suspensionPeriod.end}}' is not empty
    And  I expect '{{response.body.suspensionReason}}' is 'REORGANIZATION'
    And  I expect '{{response.body.suspensionSubreason}}' is 'MERGER'
    And  I expect '{{response.body.suspensionComment}}' is 'Suspended pending department merger'
    And  I expect '{{response.body.isSuspended}}' is "false"

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 703 - Should accept an open-ended (permanent) future suspension
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test3",
        "type": "status-test3",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2998-01-01T00:00:00Z",
          "end": null
        },
        "reason": "REORGANIZATION",
        "subreason": "MERGER"
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.suspensionPeriod.start}}' is not empty
    And  I expect '{{response.body.suspensionPeriod.end}}' is empty
    And  I expect '{{response.body.isSuspended}}' is "false"

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 704 - Should return 400 when suspensionPeriod is missing
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test4",
        "type": "status-test4",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "reason": "REORGANIZATION",
        "subreason": "MERGER"
      }
      """
    Then I expect status code is 400

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 705 - Should return 400 when the suspension period start is after its end
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test5",
        "type": "status-test5",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2999-01-01T00:00:00Z",
          "end": "2998-01-01T00:00:00Z"
        },
        "reason": "REORGANIZATION",
        "subreason": "MERGER"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.status.suspension_period_invalid'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 706 - Should return 400 when the suspension period start is in the past
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test6",
        "type": "status-test6",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2020-01-01T00:00:00Z",
          "end": "2999-01-01T00:00:00Z"
        },
        "reason": "REORGANIZATION",
        "subreason": "MERGER"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.status.suspension_start_in_past'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 707 - Should return 400 when the suspension period end is in the past
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test7",
        "type": "status-test7",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": null,
          "end": "2020-01-01T00:00:00Z"
        },
        "reason": "REORGANIZATION",
        "subreason": "MERGER"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.status.suspension_end_in_past'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 708 - Should return 404 when updating the status of an unknown organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test8",
        "type": "status-test8",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2998-01-01T00:00:00Z",
          "end": "2999-01-01T00:00:00Z"
        },
        "reason": "REORGANIZATION",
        "subreason": "MERGER"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  #################################################################
  ################## Reactivate (PUT /organizational-units/{id}/status/reactivate) #######
  #################################################################

  Scenario: 709 - Should reactivate a suspended organizational unit with a justification comment
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test9",
        "type": "status-test9",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2998-01-01T00:00:00Z",
          "end": "2999-01-01T00:00:00Z"
        },
        "reason": "REORGANIZATION",
        "subreason": "MERGER"
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Reactivated after review"
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.ouID}}'
    And  I expect '{{response.body.reactivationComment}}' is 'Reactivated after review'
    And  I expect '{{response.body.isSuspended}}' is "false"
    And  I expect '{{response.body.status}}' is 'ACTIVE'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 710 - Should return 400 when reactivating an organizational unit that is not suspended
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "status-test10",
        "type": "status-test10",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Trying to reactivate a non-suspended organizational unit"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.status.not_suspended'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 711 - Should return 404 when reactivating an unknown organizational unit
    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-4000-8000-000000000000/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Trying to reactivate an unknown organizational unit"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  #################################################################
  ################## Find accounts of O.U. (GET /organizational-units/{id}/accounts) #######
  #################################################################
  Scenario Outline: 801 - Should return <user> for <ou> organizational-unit
    When I request '{{env.E2E_API_URL}}/organizational-units?name=<ou>' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I store 'ouID' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts?externalId=<user>' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I store 'userID' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouID}}/accounts?id={{ctx.userID}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.userID}}'

    Examples:
      | user  | ou          |
      | admin | root        |
      | user1 | Company A   |
      | user2 | Company B   |
      | user3 | Division A1 |
      | user4 | Division A2 |
      | user5 | Division B1 |

  #################################################################
  ################## Hierarchy closure (organizational_unit_closures) #######
  #################################################################
  # The API neither detaches an organizational unit from its parent nor adds a second parent:
  # these relations are deleted or inserted in database.
  Scenario: 901 - Should remove the ancestor paths of a detached organizational unit and of its descendants
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # Hierarchy: root -> A -> B -> C
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "closure-901-a",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouAID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.ouAID}}",
        "name": "closure-901-b",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouBID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.ouBID}}",
        "name": "closure-901-c",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouCID' as '{{response.body.id}}' in context

    When I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouCID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is 'closure-901-a,closure-901-b,root'

    # Detaching A -> B removes every path from root and A to B and C.
    When I execute sql request "DELETE FROM organizational_unit_relations WHERE parent_id = $1 AND child_id = $2" with values:
      """
      ["{{ctx.ouAID}}", "{{ctx.ouBID}}"]
      """
    And  I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouBID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is empty

    When I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouCID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is 'closure-901-b'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouCID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouBID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouAID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 902 - Should keep the ancestor paths reached through another parent when a relation is detached
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # Hierarchy: root -> A -> C -> D and root -> B -> C
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "closure-902-a",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouAID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "closure-902-b",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouBID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.ouAID}}",
        "name": "closure-902-c",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouCID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.ouCID}}",
        "name": "closure-902-d",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouDID' as '{{response.body.id}}' in context

    When I execute sql request "INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by) SELECT $1, oun_id, created_by, created_by FROM organizational_units WHERE oun_id = $2" with values:
      """
      ["{{ctx.ouBID}}", "{{ctx.ouCID}}"]
      """
    And  I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouDID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is 'closure-902-a,closure-902-b,closure-902-c,root'

    # Detaching A -> C keeps the paths through B: root -> B -> C -> D.
    When I execute sql request "DELETE FROM organizational_unit_relations WHERE parent_id = $1 AND child_id = $2" with values:
      """
      ["{{ctx.ouAID}}", "{{ctx.ouCID}}"]
      """
    And  I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouCID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is 'closure-902-b,root'

    When I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouDID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is 'closure-902-b,closure-902-c,root'

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouDID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouCID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouBID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouAID}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 903 - Should keep the closure table consistent with the relations after successive deletions
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # Hierarchy: root -> A -> B -> C -> D and A -> C
    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "closure-903-a",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouAID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.ouAID}}",
        "name": "closure-903-b",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouBID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.ouBID}}",
        "name": "closure-903-c",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouCID' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.ouCID}}",
        "name": "closure-903-d",
        "type": "closure-test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouDID' as '{{response.body.id}}' in context

    When I execute sql request "INSERT INTO organizational_unit_relations (parent_id, child_id, created_by, updated_by) SELECT $1, oun_id, created_by, created_by FROM organizational_units WHERE oun_id = $2" with values:
      """
      ["{{ctx.ouAID}}", "{{ctx.ouCID}}"]
      """

    # Detaching A -> C keeps the paths through B: root -> A -> B -> C -> D.
    And  I execute sql request "DELETE FROM organizational_unit_relations WHERE parent_id = $1 AND child_id = $2" with values:
      """
      ["{{ctx.ouAID}}", "{{ctx.ouCID}}"]
      """
    And  I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouDID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is 'closure-903-a,closure-903-b,closure-903-c,root'

    # Deleting B removes its relations: C and D are no longer below root and A.
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouBID}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT string_agg(o.name, ',' ORDER BY o.name) AS ancestors FROM organizational_unit_closures c JOIN organizational_units o ON o.oun_id = c.ancestor_id WHERE c.descendant_id = $1 AND c.depth > 0" with values:
      """
      ["{{ctx.ouDID}}"]
      """
    Then I expect '{{ctx.dbResults[0].ancestors}}' is 'closure-903-c'

    # The closure table holds exactly the paths built from the relations.
    When I execute sql request "WITH RECURSIVE expected AS (SELECT oun_id AS ancestor_id, oun_id AS descendant_id FROM organizational_units UNION SELECT e.ancestor_id, r.child_id FROM expected e JOIN organizational_unit_relations r ON r.parent_id = e.descendant_id) (SELECT ancestor_id, descendant_id FROM organizational_unit_closures EXCEPT SELECT ancestor_id, descendant_id FROM expected) UNION ALL (SELECT ancestor_id, descendant_id FROM expected EXCEPT SELECT ancestor_id, descendant_id FROM organizational_unit_closures)"
    Then I expect 0 database results

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouDID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouCID}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouAID}}' with method 'DELETE'
    Then I expect status code is 204
