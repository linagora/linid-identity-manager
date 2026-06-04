Feature: Test API Account endpoints

  # Note: Background handles authentication before each Scenario.
  # Scenario 101 overrides the Authorization header to test 401 behavior.

  ################## Authentication #######################
  ## 101 Should return 401 without valid authentication

  ################## Create (POST /accounts) ##############
  ## 201 Should create an account with valid data
  ## 202 Should return 400 with missing required fields
  ## 203 Should return 400 with invalid email
  ## 204 Should return 500 when creating account with duplicate email
  ## 205 Should return 400 when validity period start is null
  ## 206 Should return 400 when validity period start is before current date
  ## 207 Should create an account link with an organizational unit

  ################## Find All (GET /accounts) #############
  ## 301 Should return paginated list of accounts

  ################## Find By Id (GET /accounts/{id}) ######
  ## 401 Should return 200 for existing account
  ## 402 Should return 404 for unknown account id

  ################## Delete (DELETE /accounts/{id}) #######
  ## 501 Should return 204 when deleting existing account
  ## 502 Should return 404 when deleting unknown account

  ################## Update Status (PUT /accounts/{id}/status) #####
  ## 601 Should update status fields and return updated view
  ## 602 Should update status fields and return updated view when persisted validity start is equal to new validity start
  ## 603 Should clear status fields when null values are provided
  ## 604 Should return 404 when updating status of unknown account
  ## 605 Should return 404 when no status account row exists yet
  ## 606 Should return 400 when activationAt is provided
  ## 607 Should return 400 when validity period start is null
  ## 608 Should return 400 when validity period start is after end
  ## 609 Should return 400 when suspension period start is after end
  ## 610 Should return 400 when persisted validity start is in the past and new validity start is not equal to persisted one
  ## 611 Should return 400 when new validity start is in the past
  ## 612 Should return 400 when validity end is in the past
  ## 613 Should return 400 when suspension start is before validity start
  ## 614 Should return 400 when suspension start is in the past
  ## 615 Should return 400 when suspension is outside validity end
  ## 616 Should accept open-ended validity (end null) without suspension
  ## 617 Should accept when suspension start equals persisted past suspension start (idempotent)

  ################## Activate (PUT /accounts/{id}/status/activate) #####
  ## 701 Should activate account when business rules are satisfied
  ## 702 Should return 404 when no account status row exists yet
  ## 703 Should return 400 when account is already activated
  ## 704 Should return 400 when validity period start is in the future
  ## 705 Should return 400 when activationAt is before validity start
  ## 706 Should return 400 when activationAt is in the future
  ## 707 Should return 404 when activating unknown account

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
  ################## Authentication ###################
  ####################################################

  Scenario: 101 - Should return 401 without valid authentication
    Given I set http header 'Authorization' with 'Bearer badtoken'
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-001",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 401

  ####################################################
  ################## Create (POST /accounts) ##########
  ####################################################

  Scenario: 201 - Should create an account with valid data
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-201",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john201@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I expect '{{response.body | dump}}' as 'json' to have length 9
    And   I expect '{{response.body.id}}' is not empty
    And   I expect '{{response.body.externalId}}' is 'ext-201'
    And   I expect '{{response.body.lastname}}' is 'Doe'
    And   I expect '{{response.body.firstname}}' is 'John'
    And   I expect '{{response.body.email}}' is 'john201@example.com'
    And   I expect '{{response.body.createdBy}}' is not empty
    And   I expect '{{response.body.updatedBy}}' is not empty
    And   I expect '{{response.body.insertDate}}' is not empty
    And   I expect '{{response.body.updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/accounts/{{response.body.content[0].id}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 202 - Should return 400 with missing required fields
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "",
        "lastname": "",
        "firstname": "",
        "email": "",
        "validityPeriod": null,
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.error}}' is 'Validation failed'
    And   I expect '{{response.body.errorKey}}' is 'error.validation'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 203 - Should return 400 with invalid email
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-203",
        "lastname": "Doe",
        "firstname": "Jane",
        "email": "not-an-email",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.error}}' is 'Validation failed'
    And   I expect '{{response.body.errorKey}}' is 'error.validation'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 204 - Should return 500 when creating account with duplicate email
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-204-a",
        "lastname": "Doe",
        "firstname": "John",
        "email": "duplicate204@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-204-b",
        "lastname": "Doe",
        "firstname": "Jane",
        "email": "duplicate204@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 500

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 205 - Should return 400 when validity period start is null
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-205",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john205@example.com",
        "validityPeriod": {
          "start": null,
          "end": "2030-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.creation.validity_period_start_required'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 206 - Should return 400 when validity period start is before current date
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-206",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john206@example.com",
        "validityPeriod": {
          "start": "2000-01-01T00:00:00Z",
          "end": "2030-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.creation.validity_period_start_in_past'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 207 - Should create an account link with an organizational unit
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-207",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john207@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 207
    And   I expect '{{response.body | dump}}' as 'json' to have length 9
    And   I expect '{{response.body.id}}' is not empty
    And   I expect '{{response.body.externalId}}' is 'ext-207'
    And   I expect '{{response.body.lastname}}' is 'Doe'
    And   I expect '{{response.body.firstname}}' is 'John'
    And   I expect '{{response.body.email}}' is 'john207@example.com'
    And   I expect '{{response.body.createdBy}}' is not empty
    And   I expect '{{response.body.updatedBy}}' is not empty
    And   I expect '{{response.body.insertDate}}' is not empty
    And   I expect '{{response.body.updateDate}}' is not empty

    When I request '{{env.E2E_API_URL}}/organizational-units/00000000-0000-4000-8000-00000000000a/accounts?email=john207@example.com' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content.length}}' is '1'

    When  I request '{{env.E2E_API_URL}}/accounts/{{response.body.content[0].id}}' with method 'DELETE'
    Then  I expect status code is 204

  ####################################################
  ################## Find All (GET /accounts) #########
  ####################################################

  Scenario: 301 - Should return paginated list of accounts
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-301",
        "lastname": "Find",
        "firstname": "All",
        "email": "findall301@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts?externalId=ext-301' with method 'GET'
    Then  I expect status code is 200
    And   I expect '{{response.body.totalElements}}' is '1'
    And   I expect '{{response.body.content[0].id}}' is '{{ctx.accountId}}'
    And   I expect '{{response.body.content[0].externalId}}' is 'ext-301'
    And   I expect '{{response.body.content[0].lastname}}' is 'Find'
    And   I expect '{{response.body.content[0].firstname}}' is 'All'
    And   I expect '{{response.body.content[0].email}}' is 'findall301@example.com'
    And   I expect '{{response.body.content[0].createdBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.content[0].updatedBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.content[0].insertDate}}' is not empty
    And   I expect '{{response.body.content[0].updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  ####################################################
  ################## Find By Id (GET /accounts/{id}) ##
  ####################################################

  Scenario: 401 - Should return 200 for existing account
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-401",
        "lastname": "Find",
        "firstname": "ById",
        "email": "findbyid401@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'GET'
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.accountId}}'
    And   I expect '{{response.body.externalId}}' is 'ext-401'
    And   I expect '{{response.body.lastname}}' is 'Find'
    And   I expect '{{response.body.firstname}}' is 'ById'
    And   I expect '{{response.body.email}}' is 'findbyid401@example.com'
    And   I expect '{{response.body.createdBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.updatedBy}}' is 'admin_fn admin_ln'
    And   I expect '{{response.body.insertDate}}' is not empty
    And   I expect '{{response.body.updateDate}}' is not empty

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 402 - Should return 404 for unknown account id
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000' with method 'GET'
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And   I expect '{{response.body.status}}' is '404'

  ####################################################
  ################## Delete (DELETE /accounts/{id}) ###
  ####################################################

  Scenario: 501 - Should return 204 when deleting existing account
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-501",
        "lastname": "Delete",
        "firstname": "Me",
        "email": "delete501@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'GET'
    Then  I expect status code is 404

  Scenario: 502 - Should return 404 when deleting unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000' with method 'DELETE'
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And   I expect '{{response.body.status}}' is '404'

  ####################################################
  ################## Suspend (PUT /accounts/{id}/status/suspend) ##
  ####################################################

  Scenario: 601 - Should suspend an account with a future period and reason fields
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-601",
        "lastname": "Status",
        "firstname": "Suspend",
        "email": "status601@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2090-01-01T00:00:00Z",
          "end": "2091-01-01T00:00:00Z"
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1",
        "comment": "Pending review"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.accountId}}'
    And   I expect '{{response.body.suspensionPeriod.start}}' is not empty
    And   I expect '{{response.body.suspensionPeriod.end}}' is not empty
    And   I expect '{{response.body.suspensionReason}}' is 'Suspension Reason A'
    And   I expect '{{response.body.suspensionSubreason}}' is 'Suspension Sub-reason A.1'
    And   I expect '{{response.body.suspensionComment}}' is 'Pending review'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 602 - Should accept an open-ended (permanent) future suspension
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-602",
        "lastname": "Status",
        "firstname": "Suspend",
        "email": "status602@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2090-01-01T00:00:00Z",
          "end": null
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.suspensionPeriod.start}}' is not empty
    And   I expect '{{response.body.suspensionPeriod.end}}' is empty

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 603 - Should return 400 when the suspension period start is after its end
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-603",
        "lastname": "Status",
        "firstname": "Suspend",
        "email": "status603@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2098-01-01T00:00:00Z"
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.suspension_period_invalid'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 604 - Should return 400 when the suspension period start is in the past
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-604",
        "lastname": "Status",
        "firstname": "Suspend",
        "email": "status604@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2020-01-01T00:00:00Z",
          "end": "2099-01-01T00:00:00Z"
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.suspension_start_in_past'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 605 - Should return 400 when the suspension period end is in the past
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-605",
        "lastname": "Status",
        "firstname": "Suspend",
        "email": "status605@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": null,
          "end": "2020-01-01T00:00:00Z"
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.suspension_end_in_past'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 606 - Should return 404 when suspending an unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2090-01-01T00:00:00Z",
          "end": null
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  ####################################################
  ################## Deactivate (PUT /accounts/{id}/status/deactivate) ##
  ####################################################

  Scenario: 610 - Should deactivate an account with a future date and reason fields
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-610",
        "lastname": "Status",
        "firstname": "Deactivate",
        "email": "status610@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/deactivate' with method 'PUT' with body:
      """
      {
        "deactivationAt": "2099-01-01T00:00:00Z",
        "reason": "Deactivation Reason A",
        "subreason": "Deactivation Sub-reason A.1",
        "comment": "End of contract"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.accountId}}'
    And   I expect '{{response.body.validityPeriod.end}}' is not empty
    And   I expect '{{response.body.deactivationReason}}' is 'Deactivation Reason A'
    And   I expect '{{response.body.deactivationSubreason}}' is 'Deactivation Sub-reason A.1'
    And   I expect '{{response.body.deactivationComment}}' is 'End of contract'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 611 - Should return 400 when the deactivation date is in the past
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-611",
        "lastname": "Status",
        "firstname": "Deactivate",
        "email": "status611@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/deactivate' with method 'PUT' with body:
      """
      {
        "deactivationAt": "2020-01-01T00:00:00Z",
        "reason": "Deactivation Reason A",
        "subreason": "Deactivation Sub-reason A.1"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.deactivation_in_past'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 612 - Should return 400 when the deactivation date is before the validity start
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-612",
        "lastname": "Status",
        "firstname": "Deactivate",
        "email": "status612@example.com",
        "validityPeriod": {
          "start": "2090-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/deactivate' with method 'PUT' with body:
      """
      {
        "deactivationAt": "2085-01-01T00:00:00Z",
        "reason": "Deactivation Reason A",
        "subreason": "Deactivation Sub-reason A.1"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.deactivation_before_validity_start'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 613 - Should return 404 when deactivating an unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000/status/deactivate' with method 'PUT' with body:
      """
      {
        "deactivationAt": "2099-01-01T00:00:00Z",
        "reason": "Deactivation Reason A",
        "subreason": "Deactivation Sub-reason A.1"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  ####################################################
  ################## Reactivate (PUT /accounts/{id}/status/reactivate) ##
  ####################################################

  Scenario: 620 - Should reactivate a suspended account with a justification comment
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-620",
        "lastname": "Status",
        "firstname": "Reactivate",
        "email": "status620@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2090-01-01T00:00:00Z",
          "end": null
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1"
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Investigation closed, account cleared"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.accountId}}'
    And   I expect '{{response.body.reactivationComment}}' is 'Investigation closed, account cleared'
    And   I expect '{{response.body.status}}' is 'INACTIVE'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 621 - Should return 400 when the reactivation comment is missing
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-621",
        "lastname": "Status",
        "firstname": "Reactivate",
        "email": "status621@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/suspend' with method 'PUT' with body:
      """
      {
        "suspensionPeriod": {
          "start": "2090-01-01T00:00:00Z",
          "end": null
        },
        "reason": "Suspension Reason A",
        "subreason": "Suspension Sub-reason A.1"
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": ""
      }
      """
    Then  I expect status code is 400

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 622 - Should return 400 when reactivating an account that is neither suspended nor deactivated
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-622",
        "lastname": "Status",
        "firstname": "Reactivate",
        "email": "status622@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Trying to reactivate an active account"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.nothing_to_reactivate'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 623 - Should return 404 when reactivating an unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Reactivation attempt"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  Scenario: 624 - Should re-validate a deactivated account by pushing its validity end while preserving deactivation fields
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-0000000000ce/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Re-validated after appeal",
        "validityEnd": "2099-12-31T00:00:00Z"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.status}}' is 'ACTIVE'
    And   I expect '{{response.body.reactivationComment}}' is 'Re-validated after appeal'
    And   I expect '{{response.body.deactivationReason}}' is 'Deactivation Reason A'
    And   I expect '{{response.body.deactivationSubreason}}' is 'Deactivation Sub-reason A.1'

  Scenario: 625 - Should return 400 when re-validating a deactivated account with a validity end in the past
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-0000000000cd/status/reactivate' with method 'PUT' with body:
      """
      {
        "comment": "Re-validation with a past end",
        "validityEnd": "2000-01-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.validity_end_in_past'

  ####################################################
  ################## Set validity (PUT /accounts/{id}/status/schedule-activation) ##
  ####################################################

  Scenario: 630 - Should schedule the validity period start
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-630",
        "lastname": "Status",
        "firstname": "Validity",
        "email": "status630@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/schedule-activation' with method 'PUT' with body:
      """
      {
        "validityStart": "2090-01-01T00:00:00Z"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.accountId}}'
    And   I expect '{{response.body.validityPeriod.start}}' is not empty

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 631 - Should return 400 when the validity start is not in the future
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-631",
        "lastname": "Status",
        "firstname": "Validity",
        "email": "status631@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": null
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/schedule-activation' with method 'PUT' with body:
      """
      {
        "validityStart": "2020-01-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.validity_start_not_in_future'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 632 - Should return 404 when setting validity of an unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000/status/schedule-activation' with method 'PUT' with body:
      """
      {
        "validityStart": "2090-01-01T00:00:00Z"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  ####################################################
  ################## Activate (PUT /accounts/{id}/status/activate) ##
  ####################################################

  Scenario: 701 - Should activate account when business rules are satisfied
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a002/status/activate' with method 'PUT' with body:
      """
      {
        "activationAt": "2025-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.activationAt}}' is not empty
    And   I expect '{{response.body.status}}' is 'ACTIVE'

  Scenario: 702 - Should return 404 when no account status row exists yet
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a001/status/activate' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.not_found'
    And   I expect '{{response.body.status}}' is '404'

  Scenario: 703 - Should return 400 when account is already activated
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a002/status/activate' with method 'PUT' with body:
      """
      {
        "activationAt": "2025-07-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.already_activated'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 704 - Should return 400 when validity period start is in the future
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-704",
        "lastname": "Future",
        "firstname": "Validity",
        "email": "activate704@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        },
        "organizationalUnit": "00000000-0000-4000-8000-00000000000a"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/activate' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.validity_in_future'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 705 - Should return 400 when activationAt is before validity start
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a003/status/activate' with method 'PUT' with body:
      """
      {
        "activationAt": "2020-01-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.before_validity_start'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 706 - Should return 400 when activationAt is in the future
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-00000000a004/status/activate' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.in_future'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 707 - Should return 404 when activating unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-4000-8000-000000000000/status/activate' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And   I expect '{{response.body.status}}' is '404'
