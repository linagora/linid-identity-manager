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

  ################## Find All (GET /accounts) #############
  ## 301 Should return paginated list of accounts

  ################## Find By Id (GET /accounts/{id}) ######
  ## 401 Should return 200 for existing account
  ## 402 Should return 404 for unknown account id

  ################## Delete (DELETE /accounts/{id}) #######
  ## 501 Should return 204 when deleting existing account
  ## 502 Should return 404 when deleting unknown account

  ################## Update Status (PUT /accounts/{id}/status) #####
  ## 601 Should pass-through update status fields and return updated view
  ## 602 Should clear status fields when null values are provided
  ## 603 Should return 404 when updating status of unknown account
  ## 604 Should return 400 when activationAt is provided
  ## 605 Should return 400 when validity period start is after end
  ## 606 Should return 400 when suspension period start is after end
  ## 607 Should return 400 when new validity start is in the past
  ## 608 Should return 400 when validity end is in the past
  ## 609 Should return 400 when suspension start is before validity start
  ## 610 Should return 400 when suspension is outside validity end
  ## 611 Should accept open-ended validity (end null) without suspension

  ################## Activate (PUT /accounts/{id}/status/activation) #####
  ## 701 Should activate account when business rules are satisfied (@skip — see #114)
  ## 702 Should return 404 when no account status row exists yet
  ## 703 Should return 400 when account is already activated (@skip — see #114)
  ## 704 Should return 400 when validity period start is in the future
  ## 705 Should return 400 when activationAt is before validity start (@skip — see #114)
  ## 706 Should return 404 when activating unknown account

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
        "email": "john@example.com"
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
        "email": "john201@example.com"
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

    When  I request '{{env.E2E_API_URL}}/accounts/{{response.body.id}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 202 - Should return 400 with missing required fields
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "",
        "lastname": "",
        "firstname": "",
        "email": ""
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
        "email": "not-an-email"
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
        "email": "duplicate204@example.com"
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
        "email": "duplicate204@example.com"
      }
      """
    Then  I expect status code is 500

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
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
        "email": "findall301@example.com"
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
        "email": "findbyid401@example.com"
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
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000' with method 'GET'
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
        "email": "delete501@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'GET'
    Then  I expect status code is 404

  Scenario: 502 - Should return 404 when deleting unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000' with method 'DELETE'
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And   I expect '{{response.body.status}}' is '404'

  ####################################################
  ################## Update Status (PUT /accounts/{id}/status) ##
  ####################################################

  Scenario: 601 - Should pass-through update status fields and return updated view
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-601",
        "lastname": "Status",
        "firstname": "Update",
        "email": "status601@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        },
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": "ONBOARDING",
        "statusSubreason": "INITIAL_SETUP",
        "statusComment": "Initial onboarding"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.id}}' is '{{ctx.accountId}}'
    And   I expect '{{response.body.statusReason}}' is 'ONBOARDING'
    And   I expect '{{response.body.statusSubreason}}' is 'INITIAL_SETUP'
    And   I expect '{{response.body.statusComment}}' is 'Initial onboarding'
    And   I expect '{{response.body.validityPeriod.start}}' is not empty
    And   I expect '{{response.body.validityPeriod.end}}' is not empty
    And   I expect '{{response.body.status}}' is 'INACTIVE'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 602 - Should clear status fields when null values are provided
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-602",
        "lastname": "Status",
        "firstname": "Clear",
        "email": "status602@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": "ONBOARDING",
        "statusSubreason": null,
        "statusComment": "first call"
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.statusReason}}' is empty
    And   I expect '{{response.body.statusComment}}' is empty

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 603 - Should return 404 when updating status of unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": null,
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And   I expect '{{response.body.status}}' is '404'

  Scenario: 604 - Should return 400 when activationAt is provided on PUT /status
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-604",
        "lastname": "Status",
        "firstname": "ActivationAtForbidden",
        "email": "status604@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": "2099-06-01T00:00:00Z",
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation_at_read_only'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 605 - Should return 400 when validity period start is after end
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-605",
        "lastname": "Status",
        "firstname": "ValidityInverted",
        "email": "status605@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-12-31T00:00:00Z", "end": "2099-01-01T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.validity_period_invalid'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 606 - Should return 400 when suspension period start is after end
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-606",
        "lastname": "Status",
        "firstname": "SuspensionInverted",
        "email": "status606@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": {"start": "2099-06-30T00:00:00Z", "end": "2099-06-01T00:00:00Z"},
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.suspension_period_invalid'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 607 - Should return 400 when new validity start is in the past
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-607",
        "lastname": "Status",
        "firstname": "PastStart",
        "email": "status607@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2020-01-01T00:00:00Z", "end": "2099-01-01T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.validity_start_in_past'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 608 - Should return 400 when validity end is in the past
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-608",
        "lastname": "Status",
        "firstname": "PastEnd",
        "email": "status608@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": null, "end": "2020-12-31T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.validity_end_in_past'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 609 - Should return 400 when suspension start is before validity start
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-609",
        "lastname": "Status",
        "firstname": "SuspensionBeforeValidity",
        "email": "status609@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-06-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": {"start": "2099-03-01T00:00:00Z", "end": "2099-03-15T00:00:00Z"},
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.suspension_start_before_validity_start'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 610 - Should return 400 when suspension is outside validity end
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-610",
        "lastname": "Status",
        "firstname": "SuspensionAfterEnd",
        "email": "status610@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2099-06-30T00:00:00Z"},
        "suspensionPeriod": {"start": "2099-08-01T00:00:00Z", "end": "2099-08-15T00:00:00Z"},
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.suspension_outside_validity'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 611 - Should accept open-ended validity (end null) without suspension
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-611",
        "lastname": "Status",
        "firstname": "OpenEnded",
        "email": "status611@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": null},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": "ONBOARDING",
        "statusSubreason": null,
        "statusComment": "Open-ended validity, no expiry"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.validityPeriod.start}}' is not empty
    And   I expect '{{response.body.validityPeriod.end}}' is empty

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  ####################################################
  ################## Activate (PUT /accounts/{id}/status/activation) ##
  ####################################################

  # Skipped after #110: this scenario requires a persisted account_status whose
  # validityPeriod.start is in the past, but the new status validation rules forbid
  # PUT /accounts/{id}/status from setting a past start. Re-enable once #114 lands
  # (account_status will be co-created at POST /accounts with a validity start
  # supplied at creation time, which can be set in the past via a fixture or seed).
  @skip
  Scenario: 701 - Should activate account when business rules are satisfied
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-701",
        "lastname": "Activate",
        "firstname": "Me",
        "email": "activate701@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/activation' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.activationAt}}' is not empty
    And   I expect '{{response.body.status}}' is 'ACTIVE'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 702 - Should return 404 when no account status row exists yet
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-702",
        "lastname": "NoStatus",
        "firstname": "Yet",
        "email": "activate702@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/activation' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.not_found'
    And   I expect '{{response.body.status}}' is '404'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  # Skipped after #110: the first activation step needs validityPeriod.start in the
  # past, which the new status validation rules forbid via PUT /status. Re-enable
  # once #114 lands (status seeded at account creation can carry a past start).
  @skip
  Scenario: 703 - Should return 400 when account is already activated
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-703",
        "lastname": "Already",
        "firstname": "Activated",
        "email": "activate703@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/activation' with method 'PUT' with body:
      """
      { "activationAt": "2099-06-01T00:00:00Z" }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/activation' with method 'PUT' with body:
      """
      { "activationAt": "2099-07-01T00:00:00Z" }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.already_activated'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 704 - Should return 400 when validity period start is in the future
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-704",
        "lastname": "Future",
        "firstname": "Validity",
        "email": "activate704@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-01-01T00:00:00Z", "end": "2100-01-01T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/activation' with method 'PUT' with body:
      """
      { "activationAt": "2099-06-01T00:00:00Z" }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.validity_in_future'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  # Skipped after #110: AccountActivationValidator checks validity_in_future before
  # before_validity_start, so any validity.start >= today (the only ones the new
  # status rules allow) reaches validity_in_future first and never exercises the
  # rule under test. Re-enable once #114 lands (status seeded with a past validity
  # start lets us isolate before_validity_start).
  @skip
  Scenario: 705 - Should return 400 when activationAt is before validity start
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-705",
        "lastname": "Before",
        "firstname": "Start",
        "email": "activate705@example.com"
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {"start": "2099-06-01T00:00:00Z", "end": "2099-12-31T00:00:00Z"},
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status/activation' with method 'PUT' with body:
      """
      { "activationAt": "2099-01-01T00:00:00Z" }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.before_validity_start'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 706 - Should return 404 when activating unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000/status/activation' with method 'PUT' with body:
      """
      { "activationAt": "2099-06-01T00:00:00Z" }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And   I expect '{{response.body.status}}' is '404'
