Feature: Test API Group account endpoints

  # Note: Background handles authentication before each Scenario
  #       and stores the root organizational unit ID in context.
  # Each scenario creates its own group and account, and deletes them at the end;
  # deleting an account or a group cascades to their relationships.
  # Creating an account attaches it to an organizational unit, never to a group: group membership
  # is only created through the endpoints below.

  ################## Attach (POST /groups/{groupId}/accounts) ##################
  ## 101 Should attach an account to a group
  ## 102 Should return 400 when the account is already attached to a group
  ## 103 Should return 404 when attaching to an unknown group
  ## 104 Should return 404 when attaching an unknown account to a group
  ## 105 Should return 400 with a bad request payload (missing accountId when attaching to a group)
  ## 106 Should default the relationship extra parameters when the attach request payload omits them

  ################## Find (GET /groups/{groupId}/accounts) ##################
  ## 201 Should return the accounts attached to the group with the relationship audit
  ## 202 Should return an empty page for a group without account
  ## 203 Should return 404 when listing the accounts of an unknown group
  ## 204 Should filter the accounts of the group

  ################## Detach (DELETE /groups/{groupId}/accounts/{accountId}) ##################
  ## 301 Should detach an account from a group
  ## 302 Should return 404 when the account is not attached to the group
  ## 303 Should return 404 when detaching from an unknown group
  ## 304 Should detach the accounts when the group is deleted
  ## 305 Should detach the account from all its groups when the account is deleted

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

  ###############################################################
  ################## Attach (POST /groups/{id}/accounts) ########
  ###############################################################

  Scenario: 101 - Should attach an account to a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-101",
        "name": "Group account 101",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-101",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gra-101@example.com",
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

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "extraParameters": {
          "role": "member"
        }
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.groupId}}' is '{{ctx.groupId}}'
    And  I expect '{{response.body.accountId}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.extraParameters.role}}' is 'member'
    And  I expect '{{response.body.createdBy}}' is not empty
    And  I expect '{{response.body.updatedBy}}' is not empty
    And  I expect '{{response.body.insertDate}}' is not empty
    And  I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.accountId}}'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 102 - Should return 400 when the account is already attached to a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-102",
        "name": "Group account 102",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-102",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gra-102@example.com",
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

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.group.account.already_attached'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 103 - Should return 404 when attaching to an unknown group
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000/accounts' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-4000-8000-00000000a004"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 104 - Should return 404 when attaching an unknown account to a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-104",
        "name": "Group account 104",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "00000000-0000-0000-0000-000000000000"
      }
      """
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 105 - Should return 400 with a bad request payload (missing accountId when attaching to a group)
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-105",
        "name": "Group account 105",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      {
        "extraParameters": {}
      }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Validation failed'
    And  I expect '{{response.body.errorKey}}' is 'error.validation'
    And  I expect '{{response.body.status}}' is '400'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 106 - Should default the relationship extra parameters when the attach request payload omits them
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-106",
        "name": "Group account 106",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-106",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gra-106@example.com",
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

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.extraParameters | dump}}' is '{}'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content[0].relationExtraParameters | dump}}' is '{}'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  ###############################################################
  ################## Find (GET /groups/{id}/accounts) ###########
  ###############################################################

  Scenario: 201 - Should return the accounts attached to the group with the relationship audit
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-201",
        "name": "Group account 201",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-201",
        "lastname": "Martin",
        "firstname": "Alice",
        "email": "alice-gra-201@example.com",
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

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      {
        "accountId": "{{ctx.accountId}}",
        "extraParameters": { "role": "member" }
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.accountId}}'
    And  I expect '{{response.body.content[0].externalId}}' is 'ext-gra-201'
    And  I expect '{{response.body.content[0].lastname}}' is 'Martin'
    And  I expect '{{response.body.content[0].firstname}}' is 'Alice'
    And  I expect '{{response.body.content[0].email}}' is 'alice-gra-201@example.com'
    And  I expect '{{response.body.content[0].status}}' is 'INACTIVE'
    And  I expect '{{response.body.content[0].relationExtraParameters.role}}' is 'member'
    And  I expect '{{response.body.content[0].createdBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.content[0].updatedBy}}' is 'admin_fn admin_ln'
    And  I expect '{{response.body.content[0].insertDate}}' is not empty
    And  I expect '{{response.body.content[0].updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 202 - Should return an empty page for a group without account
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-202",
        "name": "Group account 202",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'
    And  I expect '{{response.body.content.length}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 203 - Should return 404 when listing the accounts of an unknown group
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000/accounts' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 204 - Should filter the accounts of the group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-204",
        "name": "Group account 204",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-204-a",
        "lastname": "Alpha",
        "firstname": "John",
        "email": "john-gra-204a@example.com",
        "validityPeriod": { "start": "2080-01-01T00:00:00Z", "end": "2100-01-01T00:00:00Z" },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountAId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-204-b",
        "lastname": "Beta",
        "firstname": "Jane",
        "email": "jane-gra-204b@example.com",
        "validityPeriod": { "start": "2080-01-01T00:00:00Z", "end": "2100-01-01T00:00:00Z" },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountBId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      { "accountId": "{{ctx.accountAId}}" }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      { "accountId": "{{ctx.accountBId}}" }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts?sort=lastname,asc' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '2'
    And  I expect '{{response.body.content[0].lastname}}' is 'Alpha'
    And  I expect '{{response.body.content[1].lastname}}' is 'Beta'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts?lastname=Beta' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is '{{ctx.accountBId}}'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountAId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountBId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  ###############################################################
  ################## Detach (DELETE /groups/{id}/accounts/{id}) #
  ###############################################################

  Scenario: 301 - Should detach an account from a group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-301",
        "name": "Group account 301",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-301",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gra-301@example.com",
        "validityPeriod": { "start": "2080-01-01T00:00:00Z", "end": "2100-01-01T00:00:00Z" },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      { "accountId": "{{ctx.accountId}}" }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    # The account itself survives the detachment.
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 302 - Should return 404 when the account is not attached to the group
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-302",
        "name": "Group account 302",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts/00000000-0000-4000-8000-00000000a004' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.account.not_attached'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 303 - Should return 404 when detaching from an unknown group
    When I request '{{env.E2E_API_URL}}/groups/00000000-0000-0000-0000-000000000000/accounts/00000000-0000-4000-8000-00000000a004' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.not_found'

  Scenario: 304 - Should detach the accounts when the group is deleted
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-304",
        "name": "Group account 304",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-304",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gra-304@example.com",
        "validityPeriod": { "start": "2080-01-01T00:00:00Z", "end": "2100-01-01T00:00:00Z" },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}/accounts' with method 'POST' with body:
      """
      { "accountId": "{{ctx.accountId}}" }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204

    # The account survives its group and is no longer a member of anything.
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/groups' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 305 - Should detach the account from all its groups when the account is deleted
    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-305-a",
        "name": "Group account 305 A",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupAId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-account-305-b",
        "name": "Group account 305 B",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'groupBId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-305",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john-gra-305@example.com",
        "validityPeriod": { "start": "2080-01-01T00:00:00Z", "end": "2100-01-01T00:00:00Z" },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupAId}}/accounts' with method 'POST' with body:
      """
      { "accountId": "{{ctx.accountId}}" }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupBId}}/accounts' with method 'POST' with body:
      """
      { "accountId": "{{ctx.accountId}}" }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/groups' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '2'

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    # Both groups survive their member and no longer list it.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupAId}}/accounts' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupBId}}/accounts' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    # The listing joins the accounts, so it would hide a leftover relationship:
    # detaching proves the relationship rows themselves are gone.
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupAId}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.account.not_attached'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupBId}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.group.account.not_attached'

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupAId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupBId}}' with method 'DELETE'
    Then I expect status code is 204
