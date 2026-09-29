Feature: Test API Group endpoints

  # Note: Background handles authentication before each Scenario
  #       and stores the root organizational unit and LINID application IDs in context.
  # Endpoint authorization behavior is covered by a dedicated authorization feature.

  ################## Create (POST /groups) ###########################
  ## 101 Should create a group with valid data
  ## 102 Should return 400 creating a group with a bad request payload (missing <field>)
  ## 103 Should return 400 with another group with same code
  ## 104 Should return 400 with an invalid group <field>
  ## 105 Should return 404 with an unknown <reference>
  ## 106 Should create a group with empty extra parameters when they are omitted

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
  ## 504 Should return 400 updating a group with a bad request payload (missing <field>)
  ## 505 Should return 400 when a group is set as its own parent
  ## 506 Should return 400 when the parent group is a descendant of the group
  ## 507 Should return 400 updating a group with an invalid <field>
  ## 508 Should keep the extra parameters when the update payload omits them

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
        "name": "Group 101 parent",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp101ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-101",
        "name": "Group 101",
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
    And  I expect '{{response.body.name}}' is 'Group 101'
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

  Scenario Outline: 102 - Should return 400 creating a group with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": <code>,
        "name": <name>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    Examples:
      | field | code      | name        |
      | code  | null      | "Group 102" |
      | name  | "grp-102" | null        |

  Scenario: 103 - Should return 400 with another group with same code
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-103",
        "name": "Group 103",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp103Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-103",
        "name": "Another Group 103",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.code.already_exists'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp103Id}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 104 - Should return 400 with an invalid group <field>
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": <code>,
        "name": "Group 104",
        "email": <email>,
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    Examples:
      | field | code      | email          |
      | code  | "grp/104" | null           |
      | email | "grp-104" | "not-an-email" |

  Scenario Outline: 105 - Should return 404 with an unknown <reference>
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-105",
        "name": "Group 105",
        "parentId": <parentId>,
        "organizationalUnitId": <organizationalUnitId>,
        "applicationId": <applicationId>,
        "extraParameters": {}
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is '<errorKey>'

    Examples:
      | reference           | parentId                               | organizationalUnitId                   | applicationId                          | errorKey                            |
      | parent group        | "00000000-0000-0000-0000-000000000000" | null                                   | null                                   | error.group.not_found               |
      | organizational unit | null                                   | "00000000-0000-0000-0000-000000000000" | null                                   | error.organizational.unit.not_found |
      | application         | null                                   | null                                   | "00000000-0000-0000-0000-000000000000" | error.application.not_found         |

  Scenario: 106 - Should create a group with empty extra parameters when they are omitted
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-106",
        "name": "Group 106"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'
    And  I store 'grp106Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp106Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp106Id}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Find All (GET /groups) ##########
  ####################################################

  Scenario: 201 - Should return paginated list of groups with resolved relationships
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-201-parent",
        "name": "Group 201 parent",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp201ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-201",
        "name": "Group 201",
        "parentId": "{{ctx.grp201ParentId}}",
        "organizationalUnitId": "{{ctx.rootId}}",
        "applicationId": "{{ctx.linidId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp201Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups?code=grp-201' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.grp201Id}}'
    And  I expect '{{response.body.content[0].code}}' is 'grp-201'
    And  I expect '{{response.body.content[0].name}}' is 'Group 201'
    And  I expect '{{response.body.content[0].parentId}}' is '{{ctx.grp201ParentId}}'
    And  I expect '{{response.body.content[0].parentName}}' is 'Group 201 parent'
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
        "name": "Group 301",
        "description": "A group for tests",
        "email": "grp-301@example.com",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp301Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp301Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '{{ctx.grp301Id}}'
    And  I expect '{{response.body.code}}' is 'grp-301'
    And  I expect '{{response.body.name}}' is 'Group 301'
    And  I expect '{{response.body.parentId}}' is empty
    And  I expect '{{response.body.parentName}}' is empty
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
        "name": "Group 401",
        "extraParameters": {}
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
        "name": "Group 403 parent",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp403ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-403-child",
        "name": "Group 403 child",
        "parentId": "{{ctx.grp403ParentId}}",
        "extraParameters": {}
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
    And  I expect '{{response.body.parentName}}' is empty

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
        "name": "Group 501 parent",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp501ParentId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-501",
        "name": "Group 501",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp501Id' as '{{response.body.id}}' in context
    And  I store 'lastInsertDate' as '{{response.body.insertDate}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp501Id}}' with method 'PUT' with body:
      """
      {
        "code": "grp-501-updated",
        "name": "Group 501 updated",
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
    And  I expect '{{response.body.name}}' is 'Group 501 updated'
    And  I expect '{{response.body.parentId}}' is '{{ctx.grp501ParentId}}'
    And  I expect '{{response.body.parentName}}' is 'Group 501 parent'
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
        "name": "Group 502",
        "extraParameters": {}
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 503 - Should return 400 when updating with a code used by another group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-503-a",
        "name": "Group 503 A",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp503aId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-503-b",
        "name": "Group 503 B",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp503bId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp503bId}}' with method 'PUT' with body:
      """
      {
        "code": "grp-503-a",
        "name": "Group 503 B",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.code.already_exists'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp503aId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp503bId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario Outline: 504 - Should return 400 updating a group with a bad request payload (missing <field>)
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-504-<field>",
        "name": "Group 504",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp504Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp504Id}}' with method 'PUT' with body:
      """
      {
        "code": <code>,
        "name": <name>
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp504Id}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | field | code      | name        |
      | code  | null      | "Group 504" |
      | name  | "grp-504" | null        |

  Scenario: 505 - Should return 400 when a group is set as its own parent
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-505",
        "name": "Group 505",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp505Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp505Id}}' with method 'PUT' with body:
      """
      {
        "code": "grp-505",
        "name": "Group 505",
        "parentId": "{{ctx.grp505Id}}",
        "extraParameters": {}
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
        "name": "Group 506 root",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp506RootId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-506-child",
        "name": "Group 506 child",
        "parentId": "{{ctx.grp506RootId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp506ChildId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-506-grandchild",
        "name": "Group 506 grandchild",
        "parentId": "{{ctx.grp506ChildId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp506GrandchildId' as '{{response.body.id}}' in context

    # Attaching the root group under its grandchild would close a loop.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp506RootId}}' with method 'PUT' with body:
      """
      {
        "code": "grp-506-root",
        "name": "Group 506 root",
        "parentId": "{{ctx.grp506GrandchildId}}",
        "extraParameters": {}
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

  Scenario Outline: 507 - Should return 400 updating a group with an invalid <field>
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-507-<field>",
        "name": "Group 507",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'grp507Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp507Id}}' with method 'PUT' with body:
      """
      {
        "code": <code>,
        "name": "Group 507",
        "email": <email>,
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp507Id}}' with method 'DELETE'
    Then I expect status code is 204

    Examples:
      | field | code      | email          |
      | code  | "grp/507" | null           |
      | email | "grp-507" | "not-an-email" |

  Scenario: 508 - Should keep the extra parameters when the update payload omits them
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-508",
        "name": "Group 508",
        "extraParameters": { "test": "test" }
      }
      """
    Then I expect status code is 201
    And  I store 'grp508Id' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp508Id}}' with method 'PUT' with body:
      """
      {
        "code": "grp-508",
        "name": "Group 508 updated"
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp508Id}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.extraParameters | dump}}' is '{"test":"test"}'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp508Id}}' with method 'DELETE'
    Then I expect status code is 204
