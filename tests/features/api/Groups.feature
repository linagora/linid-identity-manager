Feature: Test API Group endpoints

  # Note: Background handles authentication before each Scenario
  #       and stores the root organizational unit and LINID application IDs in context.
  # Endpoint authorization behavior is covered by a dedicated authorization feature.

  ################## Create (POST /groups) ###########################
  ## 101 Should create a group with valid data
  ## 102 Should return 400 with a bad request payload (missing <field>)
  ## 103 Should return 400 with another group with same code
  ## 104 Should return 400 with invalid group code
  ## 105 Should return 400 with invalid group email
  ## 106 Should return 404 with unknown parent group
  ## 107 Should return 404 with unknown organizational unit
  ## 108 Should return 404 with unknown application

  ################## Find All (GET /groups) ##########################
  ## 201 Should return paginated list of groups with resolved relationships

  ################## Find By Id (GET /groups/{id}) ###################
  ## 301 Should return 200 for existing group
  ## 302 Should return 404 for unknown group id

  ################## Delete (DELETE /groups/{id}) ####################
  ## 401 Should return 204 when deleting existing group
  ## 402 Should return 404 when deleting unknown group
  ## 403 Should detach child groups when deleting their parent group

  ################## Update (PUT /groups/{id}) #######################
  ## 501 Should return 200 updating a group
  ## 502 Should return 404 when updating an unknown group
  ## 503 Should return 400 when updating with a code used by another group
  ## 504 Should return 400 with a bad request payload (missing <field>)
  ## 505 Should return 400 when a group is set as its own parent
  ## 506 Should return 400 when the parent group is a descendant of the group

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
    And  I store 'rootId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I store 'linidId' as '{{response.body.content[0].id}}' in context

  ####################################################
  ################## Create (POST /groups) ###########
  ####################################################

  Scenario: 101 - Should create a group with valid data
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-101-parent",
        "label": "Group 101 parent"
      }
      """
    Then I expect status code is 201
    And  I store 'grp101ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-101",
        "label": "Group 101",
        "parentId": "{{ctx.grp101ParentId}}",
        "description": "A group for tests",
        "email": "grp-101@example.com",
        "organizationalUnitId": "{{ctx.rootId}}",
        "applicationId": "{{ctx.linidId}}",
        "extraParameters": { "test": "test" }
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.code}}' is 'grp-101'
    And  I expect '{{response.body.label}}' is 'Group 101'
    And  I expect '{{response.body.parentId}}' is '{{ctx.grp101ParentId}}'
    And  I expect '{{response.body.description}}' is 'A group for tests'
    And  I expect '{{response.body.email}}' is 'grp-101@example.com'
    And  I expect '{{response.body.organizationalUnitId}}' is '{{ctx.rootId}}'
    And  I expect '{{response.body.applicationId}}' is '{{ctx.linidId}}'
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'
    And  I expect '{{response.body.createdBy}}' is not empty
    And  I expect '{{response.body.updatedBy}}' is not empty
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/groups/{{response.body.id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp101ParentId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 102 - Should return 400 with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": <code>,
        "label": <label>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    Examples:
      | field | code      | label       |
      | code  | null      | "Group 102" |
      | label | "grp-102" | null        |

  Scenario: 103 - Should return 400 with another group with same code
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-103",
        "label": "Group 103"
      }
      """
    Then I expect status code is 201
    And  I store 'grp103Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-103",
        "label": "Another Group 103"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.code.already_exists'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp103Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 104 - Should return 400 with invalid group code
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp/104",
        "label": "Group 104"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 105 - Should return 400 with invalid group email
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-105",
        "label": "Group 105",
        "email": "not-an-email"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 106 - Should return 404 with unknown parent group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-106",
        "label": "Group 106",
        "parentId": "00000000-0000-0000-0000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 107 - Should return 404 with unknown organizational unit
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-107",
        "label": "Group 107",
        "organizationalUnitId": "00000000-0000-0000-0000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.organizational.unit.not_found'

  Scenario: 108 - Should return 404 with unknown application
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-108",
        "label": "Group 108",
        "applicationId": "00000000-0000-0000-0000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.application.not_found'

  ####################################################
  ################## Find All (GET /groups) ##########
  ####################################################

  Scenario: 201 - Should return paginated list of groups with resolved relationships
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-201-parent",
        "label": "Group 201 parent"
      }
      """
    Then I expect status code is 201
    And  I store 'grp201ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-201",
        "label": "Group 201",
        "parentId": "{{ctx.grp201ParentId}}",
        "organizationalUnitId": "{{ctx.rootId}}",
        "applicationId": "{{ctx.linidId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp201Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups?code=grp-201' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.grp201Id}}'
    And  I expect '{{response.body.content[0].code}}' is 'grp-201'
    And  I expect '{{response.body.content[0].label}}' is 'Group 201'
    And  I expect '{{response.body.content[0].parentId}}' is '{{ctx.grp201ParentId}}'
    And  I expect '{{response.body.content[0].parentLabel}}' is 'Group 201 parent'
    And  I expect '{{response.body.content[0].organizationalUnitId}}' is '{{ctx.rootId}}'
    And  I expect '{{response.body.content[0].organizationalUnitName}}' is 'root'
    And  I expect '{{response.body.content[0].applicationId}}' is '{{ctx.linidId}}'
    And  I expect '{{response.body.content[0].applicationName}}' is 'LINID - Identity Manager'
    And  I expect '{{response.body.content[0].extraParameters | dump}}' is '{}'
    And  I expect '{{response.body.content[0].createdBy}}' is 'admin_fn admin_ln'

    When I request '{{env.E2E_API_URL}}/groups?parentId={{ctx.grp201ParentId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.grp201Id}}'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp201Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp201ParentId}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Find By Id (GET /groups/{id}) ###
  ####################################################

  Scenario: 301 - Should return 200 for existing group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-301",
        "label": "Group 301",
        "description": "A group for tests",
        "email": "grp-301@example.com"
      }
      """
    Then I expect status code is 201
    And  I store 'grp301Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp301Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.grp301Id}}'
    And  I expect '{{response.body.code}}' is 'grp-301'
    And  I expect '{{response.body.label}}' is 'Group 301'
    And  I expect '{{response.body.parentId}}' is empty
    And  I expect '{{response.body.parentLabel}}' is empty
    And  I expect '{{response.body.description}}' is 'A group for tests'
    And  I expect '{{response.body.email}}' is 'grp-301@example.com'
    And  I expect '{{response.body.organizationalUnitId}}' is empty
    And  I expect '{{response.body.applicationId}}' is empty
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'
    And  I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp301Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should return 404 for unknown group id
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  ####################################################
  ################## Delete (DELETE /groups/{id}) ####
  ####################################################

  Scenario: 401 - Should return 204 when deleting existing group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-401",
        "label": "Group 401"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{response.body.id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 402 - Should return 404 when deleting unknown group
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 403 - Should detach child groups when deleting their parent group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-403-parent",
        "label": "Group 403 parent"
      }
      """
    Then I expect status code is 201
    And  I store 'grp403ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-403-child",
        "label": "Group 403 child",
        "parentId": "{{ctx.grp403ParentId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp403ChildId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp403ParentId}}' with method 'DELETE'
    Then I expect status code is 204

    # The child group survives its parent and becomes a top-level group.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp403ChildId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.parentId}}' is empty
    And  I expect '{{response.body.parentLabel}}' is empty

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp403ChildId}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Update (PUT /groups/{id}) #######
  ####################################################

  Scenario: 501 - Should return 200 updating a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-501-parent",
        "label": "Group 501 parent"
      }
      """
    Then I expect status code is 201
    And  I store 'grp501ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-501",
        "label": "Group 501"
      }
      """
    Then I expect status code is 201
    And  I store 'grp501Id' as '{{response.body.id}}' in context
    And  I store 'lastInsertDate' as '{{response.body.insertDate}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp501Id}}' with method 'PUT' with body:
      """
      {
        "code": "grp-501-updated",
        "label": "Group 501 updated",
        "parentId": "{{ctx.grp501ParentId}}",
        "description": "Updated description",
        "email": "grp-501@example.com",
        "organizationalUnitId": "{{ctx.rootId}}",
        "applicationId": "{{ctx.linidId}}",
        "extraParameters": { "test": "test" }
      }
      """
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp501Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.grp501Id}}'
    And  I expect '{{response.body.code}}' is 'grp-501-updated'
    And  I expect '{{response.body.label}}' is 'Group 501 updated'
    And  I expect '{{response.body.parentId}}' is '{{ctx.grp501ParentId}}'
    And  I expect '{{response.body.parentLabel}}' is 'Group 501 parent'
    And  I expect '{{response.body.description}}' is 'Updated description'
    And  I expect '{{response.body.email}}' is 'grp-501@example.com'
    And  I expect '{{response.body.organizationalUnitId}}' is '{{ctx.rootId}}'
    And  I expect '{{response.body.organizationalUnitName}}' is 'root'
    And  I expect '{{response.body.applicationId}}' is '{{ctx.linidId}}'
    And  I expect '{{response.body.applicationName}}' is 'LINID - Identity Manager'
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'
    And  I expect '{{response.body.insertDate}}' is "{{ctx.lastInsertDate}}"
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp501Id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp501ParentId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 502 - Should return 404 when updating an unknown group
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000' with method 'PUT' with body:
      """
      {
        "code": "grp-502",
        "label": "Group 502"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 503 - Should return 400 when updating with a code used by another group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-503-a",
        "label": "Group 503 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp503aId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-503-b",
        "label": "Group 503 B"
      }
      """
    Then I expect status code is 201
    And  I store 'grp503bId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp503bId}}' with method 'PUT' with body:
      """
      {
        "code": "grp-503-a",
        "label": "Group 503 B"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.code.already_exists'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp503aId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp503bId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 504 - Should return 400 with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-504-<field>",
        "label": "Group 504"
      }
      """
    Then I expect status code is 201
    And  I store 'grp504Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp504Id}}' with method 'PUT' with body:
      """
      {
        "code": <code>,
        "label": <label>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp504Id}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | field | code      | label       |
      | code  | null      | "Group 504" |
      | label | "grp-504" | null        |

  Scenario: 505 - Should return 400 when a group is set as its own parent
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-505",
        "label": "Group 505"
      }
      """
    Then I expect status code is 201
    And  I store 'grp505Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp505Id}}' with method 'PUT' with body:
      """
      {
        "code": "grp-505",
        "label": "Group 505",
        "parentId": "{{ctx.grp505Id}}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.parent.cycle'

    # The group must be left untouched.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp505Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.parentId}}' is empty

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp505Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 506 - Should return 400 when the parent group is a descendant of the group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-506-root",
        "label": "Group 506 root"
      }
      """
    Then I expect status code is 201
    And  I store 'grp506RootId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-506-child",
        "label": "Group 506 child",
        "parentId": "{{ctx.grp506RootId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp506ChildId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-506-grandchild",
        "label": "Group 506 grandchild",
        "parentId": "{{ctx.grp506ChildId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp506GrandchildId' as '{{response.body.id}}' in context

    # Attaching the root group under its grandchild would close a loop.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp506RootId}}' with method 'PUT' with body:
      """
      {
        "code": "grp-506-root",
        "label": "Group 506 root",
        "parentId": "{{ctx.grp506GrandchildId}}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.parent.cycle'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp506GrandchildId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp506ChildId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp506RootId}}' with method 'DELETE'
    Then I expect status code is 204
