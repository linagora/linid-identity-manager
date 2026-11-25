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

  ################## Closure paths (group_closures) ###################
  ## 601 Should store the closure paths of a new group hierarchy
  ## 602 Should rebuild the closure paths of a subtree moved under another parent
  ## 603 Should remove the closure paths through a deleted group
  ## 604 Should reject in the database a parent change closing a cycle
  ## 605 Should reject in the database an insert closing a cycle
  ## 606 Should return 400 when the database rejects a cycle missed by the API check
  ## 607 Should hold the lock of the group hierarchy until the end of the transaction
  ## 608 Should rebuild the closure paths of groups moved by a single multi-row UPDATE
  ## 609 Should rebuild the closure paths after a single multi-row DELETE of groups
  ## 610 Should not take the lock of the group hierarchy on an update keeping the parent
  ## 611 Should reject a change of the group hierarchy outside READ COMMITTED

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

  ####################################################
  ################## Closure paths (group_closures) ##
  ####################################################

  Scenario: 601 - Should store the closure paths of a new group hierarchy
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A -> B -> C
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-601-a",
        "name": "Group 601 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp601AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-601-b",
        "name": "Group 601 B",
        "parentId": "{{ctx.grp601AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp601BId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-601-c",
        "name": "Group 601 C",
        "parentId": "{{ctx.grp601BId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp601CId' as '{{response.body.id}}' in context

    When I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth DESC, g.code) AS ancestors FROM group_closures c JOIN groups g ON g.grp_id = c.ancestor_id WHERE c.descendant_id = $1" with values:
      """
      ["{{ctx.grp601CId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ancestors}}' is 'grp-601-a:2, grp-601-b:1, grp-601-c:0'

    When I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth, g.code) AS descendants FROM group_closures c JOIN groups g ON g.grp_id = c.descendant_id WHERE c.ancestor_id = $1" with values:
      """
      ["{{ctx.grp601AId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].descendants}}' is 'grp-601-a:0, grp-601-b:1, grp-601-c:2'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp601CId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp601BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp601AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 602 - Should rebuild the closure paths of a subtree moved under another parent
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A -> B -> C and D
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-602-a",
        "name": "Group 602 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp602AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-602-b",
        "name": "Group 602 B",
        "parentId": "{{ctx.grp602AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp602BId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-602-c",
        "name": "Group 602 C",
        "parentId": "{{ctx.grp602BId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp602CId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-602-d",
        "name": "Group 602 D"
      }
      """
    Then I expect status code is 201
    And  I store 'grp602DId' as '{{response.body.id}}' in context

    # Moving B under D moves C with it, and leaves A without descendants.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp602BId}}' with method 'PUT' with body:
      """
      {
        "code": "grp-602-b",
        "name": "Group 602 B",
        "parentId": "{{ctx.grp602DId}}"
      }
      """
    Then I expect status code is 200

    When I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth DESC, g.code) AS ancestors FROM group_closures c JOIN groups g ON g.grp_id = c.ancestor_id WHERE c.descendant_id = $1" with values:
      """
      ["{{ctx.grp602CId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ancestors}}' is 'grp-602-d:2, grp-602-b:1, grp-602-c:0'

    When I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth, g.code) AS descendants FROM group_closures c JOIN groups g ON g.grp_id = c.descendant_id WHERE c.ancestor_id = $1" with values:
      """
      ["{{ctx.grp602AId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].descendants}}' is 'grp-602-a:0'

    # group_closures matches the closure recomputed from groups.parent_id.
    When I execute sql request "WITH RECURSIVE expected AS (SELECT grp_id AS ancestor_id, grp_id AS descendant_id, 0 AS depth FROM groups UNION ALL SELECT g.parent_id, e.descendant_id, e.depth + 1 FROM expected e JOIN groups g ON g.grp_id = e.ancestor_id WHERE g.parent_id IS NOT NULL) CYCLE ancestor_id SET is_cycle USING path SELECT count(*) AS total FROM ((SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle EXCEPT SELECT ancestor_id, descendant_id, depth FROM group_closures) UNION ALL (SELECT ancestor_id, descendant_id, depth FROM group_closures EXCEPT SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle)) AS diff"
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp602CId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp602BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp602DId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp602AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 603 - Should remove the closure paths through a deleted group
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A -> B -> C
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-603-a",
        "name": "Group 603 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp603AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-603-b",
        "name": "Group 603 B",
        "parentId": "{{ctx.grp603AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp603BId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-603-c",
        "name": "Group 603 C",
        "parentId": "{{ctx.grp603BId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp603CId' as '{{response.body.id}}' in context

    # Deleting B makes C a top-level group: the path from A to C goes with B.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp603BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth DESC, g.code) AS ancestors FROM group_closures c JOIN groups g ON g.grp_id = c.ancestor_id WHERE c.descendant_id = $1" with values:
      """
      ["{{ctx.grp603CId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ancestors}}' is 'grp-603-c:0'

    When I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth, g.code) AS descendants FROM group_closures c JOIN groups g ON g.grp_id = c.descendant_id WHERE c.ancestor_id = $1" with values:
      """
      ["{{ctx.grp603AId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].descendants}}' is 'grp-603-a:0'

    When I execute sql request "SELECT count(*) AS total FROM group_closures WHERE ancestor_id = $1 OR descendant_id = $1" with values:
      """
      ["{{ctx.grp603BId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '0'

    # group_closures matches the closure recomputed from groups.parent_id.
    When I execute sql request "WITH RECURSIVE expected AS (SELECT grp_id AS ancestor_id, grp_id AS descendant_id, 0 AS depth FROM groups UNION ALL SELECT g.parent_id, e.descendant_id, e.depth + 1 FROM expected e JOIN groups g ON g.grp_id = e.ancestor_id WHERE g.parent_id IS NOT NULL) CYCLE ancestor_id SET is_cycle USING path SELECT count(*) AS total FROM ((SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle EXCEPT SELECT ancestor_id, descendant_id, depth FROM group_closures) UNION ALL (SELECT ancestor_id, descendant_id, depth FROM group_closures EXCEPT SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle)) AS diff"
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp603CId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp603AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 604 - Should reject in the database a parent change closing a cycle
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A -> B -> C
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-604-a",
        "name": "Group 604 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp604AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-604-b",
        "name": "Group 604 B",
        "parentId": "{{ctx.grp604AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp604BId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-604-c",
        "name": "Group 604 C",
        "parentId": "{{ctx.grp604BId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp604CId' as '{{response.body.id}}' in context

    # Without the API, attaching A under C must fail with check_violation: the block raises 'cycle accepted' otherwise.
    When I execute sql request "DO $$ BEGIN UPDATE groups SET parent_id = '{{ctx.grp604CId}}' WHERE grp_id = '{{ctx.grp604AId}}'; RAISE EXCEPTION 'cycle accepted'; EXCEPTION WHEN check_violation THEN NULL; END $$"
    And  I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth DESC, g.code) AS ancestors FROM group_closures c JOIN groups g ON g.grp_id = c.ancestor_id WHERE c.descendant_id = $1" with values:
      """
      ["{{ctx.grp604AId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ancestors}}' is 'grp-604-a:0'

    When I execute sql request "SELECT count(*) AS total FROM groups WHERE grp_id = $1 AND parent_id IS NULL" with values:
      """
      ["{{ctx.grp604AId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '1'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp604CId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp604BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp604AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 605 - Should reject in the database an insert closing a cycle
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # Two groups inserted by one statement, each the parent of the other: the foreign keys are only checked at the end
    # of the statement, so only the trigger can reject them. The block raises 'cycle accepted' otherwise.
    When I execute sql request "DO $$ DECLARE a UUID := gen_random_uuid(); b UUID := gen_random_uuid(); BEGIN INSERT INTO groups (grp_id, code, name, parent_id) VALUES (a, 'grp-605-a', 'Group 605 A', b), (b, 'grp-605-b', 'Group 605 B', a); RAISE EXCEPTION 'cycle accepted'; EXCEPTION WHEN check_violation THEN NULL; END $$"
    And  I execute sql request "SELECT count(*) AS total FROM groups WHERE code IN ('grp-605-a', 'grp-605-b')"
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '0'

  Scenario: 606 - Should return 400 when the database rejects a cycle missed by the API check
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A -> B
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-606-a",
        "name": "Group 606 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp606AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-606-b",
        "name": "Group 606 B",
        "parentId": "{{ctx.grp606AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp606BId' as '{{response.body.id}}' in context

    # The steps run one after the other: no concurrent change can be committed between the API check and its flush.
    # Removing the path from A to B makes the API check read the hierarchy as such a change would leave it: only the
    # group_closures_refresh() trigger, which walks groups, sees that A under B closes a cycle.
    When I execute sql request "DELETE FROM group_closures WHERE ancestor_id = $1 AND descendant_id = $2" with values:
      """
      ["{{ctx.grp606AId}}", "{{ctx.grp606BId}}"]
      """

    And  I request '{{env.E2E_API_URL}}/groups/{{ctx.grp606AId}}' with method 'PUT' with body:
      """
      {
        "code": "grp-606-a",
        "name": "Group 606 A",
        "parentId": "{{ctx.grp606BId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.parent.cycle'

    # The group must be left untouched.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp606AId}}' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.parentId}}' is empty

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp606BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp606AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 607 - Should hold the lock of the group hierarchy until the end of the transaction
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-607-a",
        "name": "Group 607 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp607AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-607-b",
        "name": "Group 607 B"
      }
      """
    Then I expect status code is 201
    And  I store 'grp607BId' as '{{response.body.id}}' in context

    # Unlike the lock of AccountRecompute.feature, which the API only reads, the triggers wait for this one: holding it
    # from this session would block the API request, and the scenario with it. Each block checks instead, within the
    # transaction of a change of the hierarchy, that the lock named by the OID of group_closures is held, and raises
    # 'lock not held' otherwise.
    When I execute sql request "DO $$ BEGIN UPDATE groups SET parent_id = '{{ctx.grp607AId}}' WHERE grp_id = '{{ctx.grp607BId}}'; IF NOT EXISTS (SELECT 1 FROM pg_locks WHERE locktype = 'advisory' AND classid = 'group_closures'::regclass::oid AND objid = 0 AND objsubid = 2 AND pid = pg_backend_pid() AND granted) THEN RAISE EXCEPTION 'lock not held after a parent change'; END IF; END $$"
    And  I execute sql request "DO $$ BEGIN INSERT INTO groups (code, name, parent_id) VALUES ('grp-607-c', 'Group 607 C', '{{ctx.grp607AId}}'); IF NOT EXISTS (SELECT 1 FROM pg_locks WHERE locktype = 'advisory' AND classid = 'group_closures'::regclass::oid AND objid = 0 AND objsubid = 2 AND pid = pg_backend_pid() AND granted) THEN RAISE EXCEPTION 'lock not held after an insert'; END IF; END $$"

    # C has no child, so no parent change follows its deletion: only the statement-level trigger, which takes the lock
    # before the group is locked, can hold it.
    And  I execute sql request "DO $$ BEGIN DELETE FROM groups WHERE code = 'grp-607-c'; IF NOT EXISTS (SELECT 1 FROM pg_locks WHERE locktype = 'advisory' AND classid = 'group_closures'::regclass::oid AND objid = 0 AND objsubid = 2 AND pid = pg_backend_pid() AND granted) THEN RAISE EXCEPTION 'lock not held after a delete'; END IF; END $$"

    # Released at the end of the transaction: a lock kept by the session would block every later change of the hierarchy.
    And  I execute sql request "SELECT count(*) AS total FROM pg_locks WHERE locktype = 'advisory' AND classid = 'group_closures'::regclass::oid AND objid = 0 AND pid = pg_backend_pid()"
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp607BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp607AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 608 - Should rebuild the closure paths of groups moved by a single multi-row UPDATE
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A, B -> C and D
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-608-a",
        "name": "Group 608 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp608AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-608-b",
        "name": "Group 608 B"
      }
      """
    Then I expect status code is 201
    And  I store 'grp608BId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-608-c",
        "name": "Group 608 C",
        "parentId": "{{ctx.grp608BId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp608CId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-608-d",
        "name": "Group 608 D"
      }
      """
    Then I expect status code is 201
    And  I store 'grp608DId' as '{{response.body.id}}' in context

    # One statement moves B under A and D under C, so that D ends up under the moved B: the row triggers fire once
    # both rows are written, in no fixed order.
    When I execute sql request "UPDATE groups SET parent_id = CASE WHEN grp_id = $1 THEN $2::uuid ELSE $3::uuid END WHERE grp_id IN ($1, $4)" with values:
      """
      ["{{ctx.grp608BId}}", "{{ctx.grp608AId}}", "{{ctx.grp608CId}}", "{{ctx.grp608DId}}"]
      """
    And  I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth DESC, g.code) AS ancestors FROM group_closures c JOIN groups g ON g.grp_id = c.ancestor_id WHERE c.descendant_id = $1" with values:
      """
      ["{{ctx.grp608DId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ancestors}}' is 'grp-608-a:3, grp-608-b:2, grp-608-c:1, grp-608-d:0'

    # group_closures matches the closure recomputed from groups.parent_id.
    When I execute sql request "WITH RECURSIVE expected AS (SELECT grp_id AS ancestor_id, grp_id AS descendant_id, 0 AS depth FROM groups UNION ALL SELECT g.parent_id, e.descendant_id, e.depth + 1 FROM expected e JOIN groups g ON g.grp_id = e.ancestor_id WHERE g.parent_id IS NOT NULL) CYCLE ancestor_id SET is_cycle USING path SELECT count(*) AS total FROM ((SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle EXCEPT SELECT ancestor_id, descendant_id, depth FROM group_closures) UNION ALL (SELECT ancestor_id, descendant_id, depth FROM group_closures EXCEPT SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle)) AS diff"
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp608DId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp608CId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp608BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp608AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 609 - Should rebuild the closure paths after a single multi-row DELETE of groups
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A -> B -> C -> E and A -> D
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-609-a",
        "name": "Group 609 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp609AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-609-b",
        "name": "Group 609 B",
        "parentId": "{{ctx.grp609AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp609BId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-609-c",
        "name": "Group 609 C",
        "parentId": "{{ctx.grp609BId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp609CId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-609-e",
        "name": "Group 609 E",
        "parentId": "{{ctx.grp609CId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp609EId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-609-d",
        "name": "Group 609 D",
        "parentId": "{{ctx.grp609AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp609DId' as '{{response.body.id}}' in context

    # One statement deletes A and B: the foreign key moves C and D to the root, and E stays under C.
    When I execute sql request "DELETE FROM groups WHERE grp_id IN ($1, $2)" with values:
      """
      ["{{ctx.grp609AId}}", "{{ctx.grp609BId}}"]
      """
    And  I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth DESC, g.code) AS ancestors FROM group_closures c JOIN groups g ON g.grp_id = c.ancestor_id WHERE c.descendant_id = $1" with values:
      """
      ["{{ctx.grp609EId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ancestors}}' is 'grp-609-c:1, grp-609-e:0'

    When I execute sql request "SELECT string_agg(g.code || ':' || c.depth, ', ' ORDER BY c.depth DESC, g.code) AS ancestors FROM group_closures c JOIN groups g ON g.grp_id = c.ancestor_id WHERE c.descendant_id = $1" with values:
      """
      ["{{ctx.grp609DId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].ancestors}}' is 'grp-609-d:0'

    # group_closures matches the closure recomputed from groups.parent_id.
    When I execute sql request "WITH RECURSIVE expected AS (SELECT grp_id AS ancestor_id, grp_id AS descendant_id, 0 AS depth FROM groups UNION ALL SELECT g.parent_id, e.descendant_id, e.depth + 1 FROM expected e JOIN groups g ON g.grp_id = e.ancestor_id WHERE g.parent_id IS NOT NULL) CYCLE ancestor_id SET is_cycle USING path SELECT count(*) AS total FROM ((SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle EXCEPT SELECT ancestor_id, descendant_id, depth FROM group_closures) UNION ALL (SELECT ancestor_id, descendant_id, depth FROM group_closures EXCEPT SELECT ancestor_id, descendant_id, depth FROM expected WHERE NOT is_cycle)) AS diff"
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp609EId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp609CId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp609DId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 610 - Should not take the lock of the group hierarchy on an update keeping the parent
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    # A -> B
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-610-a",
        "name": "Group 610 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp610AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-610-b",
        "name": "Group 610 B",
        "parentId": "{{ctx.grp610AId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'grp610BId' as '{{response.body.id}}' in context

    # This session holds the lock of the group hierarchy while B is renamed under the same parent. Hibernate writes
    # only the changed columns (@DynamicUpdate on Group), so parent_id is left out of the UPDATE and
    # tg_groups_hierarchy_lock does not fire. Writing every column would wait for this lock: the request would time out.
    When I execute sql request "SELECT pg_advisory_lock('group_closures'::regclass::oid::int, 0)"
    And  I request '{{env.E2E_API_URL}}/groups/{{ctx.grp610BId}}' with method 'PUT' with body:
      """
      {
        "code": "grp-610-b",
        "name": "Group 610 B renamed",
        "parentId": "{{ctx.grp610AId}}"
      }
      """
    Then I expect status code is 200
    And  I expect '{{response.body.name}}' is 'Group 610 B renamed'
    And  I expect '{{response.body.parentId}}' is '{{ctx.grp610AId}}'

    When I execute sql request "SELECT pg_advisory_unlock('group_closures'::regclass::oid::int, 0) AS released"
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].released}}' is 'true'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp610BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp610AId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 611 - Should reject a change of the group hierarchy outside READ COMMITTED
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-611-a",
        "name": "Group 611 A"
      }
      """
    Then I expect status code is 201
    And  I store 'grp611AId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-611-b",
        "name": "Group 611 B"
      }
      """
    Then I expect status code is 201
    And  I store 'grp611BId' as '{{response.body.id}}' in context

    # The steps share one connection: the transaction spans the three statements. Under REPEATABLE READ, the closure
    # triggers would keep reading the snapshot taken before waiting for the lock: attaching B under A must fail with
    # invalid_transaction_state. The block raises 'change accepted' otherwise.
    When I execute sql request "BEGIN ISOLATION LEVEL REPEATABLE READ"
    And  I execute sql request "DO $$ BEGIN UPDATE groups SET parent_id = '{{ctx.grp611AId}}' WHERE grp_id = '{{ctx.grp611BId}}'; RAISE EXCEPTION 'change accepted'; EXCEPTION WHEN invalid_transaction_state THEN NULL; END $$"
    And  I execute sql request "ROLLBACK"
    And  I execute sql request "SELECT count(*) AS total FROM groups WHERE grp_id = $1 AND parent_id IS NULL" with values:
      """
      ["{{ctx.grp611BId}}"]
      """
    Then I expect 1 database results
    And  I expect '{{ctx.dbResults[0].total}}' is '1'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp611BId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.grp611AId}}' with method 'DELETE'
    Then I expect status code is 204
