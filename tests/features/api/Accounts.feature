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

  ################## Activate (PUT /accounts/{id}/status/activation) #####
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
        }
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
        }
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
        "email": "",
        "validityPeriod": null
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
        }
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
        }
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
        }
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
        }
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
        }
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.creation.validity_period_start_in_past'
    And   I expect '{{response.body.status}}' is '400'

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
        }
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
        }
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
        "email": "delete501@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        }
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

  Scenario: 601 - Should update status fields and return updated view
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-601",
        "lastname": "Status",
        "firstname": "Update",
        "email": "status601@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        }
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

  Scenario: 602 - Should update status fields and return updated view when persisted validity start is equal to new validity start
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a002' with method 'GET'
    Then  I expect status code is 200
    And   I store 'validityPeriodStart' as '{{response.body.validityPeriod.start}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a002/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "{{ctx.validityPeriodStart}}",
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
    And   I expect '{{response.body.id}}' is '00000000-0000-0000-0000-00000000a002'
    And   I expect '{{response.body.statusReason}}' is 'ONBOARDING'
    And   I expect '{{response.body.statusSubreason}}' is 'INITIAL_SETUP'
    And   I expect '{{response.body.statusComment}}' is 'Initial onboarding'
    And   I expect '{{response.body.validityPeriod.start}}' is '{{ctx.validityPeriodStart}}'
    And   I expect '{{response.body.validityPeriod.end}}' is '2099-12-31T00:00:00Z'
    And   I expect '{{response.body.status}}' is 'INACTIVE'

  Scenario: 603 - Should clear status fields when null values are provided
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-603",
        "lastname": "Status",
        "firstname": "Clear",
        "email": "status603@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        }
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
        "statusSubreason": null,
        "statusComment": "first call"
      }
      """
    Then  I expect status code is 200

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        },
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

  Scenario: 604 - Should return 404 when updating status of unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        },
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

  Scenario: 605 - Should return 404 when no status account row exists yet
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a001/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        },
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.not_found'
    And   I expect '{{response.body.status}}' is '404'

  Scenario: 606 - Should return 400 when activationAt is provided on PUT /status
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-606",
        "lastname": "Status",
        "firstname": "ActivationAtForbidden",
        "email": "status606@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
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

  Scenario: 607 - Should return 400 when validity period start is null
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-607",
        "lastname": "Status",
        "firstname": "ValidityStartNull",
        "email": "status607@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": null,
          "end": "2099-12-31T00:00:00Z"
        },
        "suspensionPeriod": null,
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.validity_period_start_required'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 608 - Should return 400 when validity period start is after end
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-608",
        "lastname": "Status",
        "firstname": "ValidityInverted",
        "email": "status608@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-12-31T00:00:00Z",
          "end": "2099-01-01T00:00:00Z"
        },
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

  Scenario: 609 - Should return 400 when suspension period start is after end
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-609",
        "lastname": "Status",
        "firstname": "SuspensionInverted",
        "email": "status609@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
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
        "suspensionPeriod": {
          "start": "2099-06-30T00:00:00Z",
          "end": "2099-06-01T00:00:00Z"
        },
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

  Scenario: 610 - Should return 400 when persisted validity start is in the past and new validity start is not equal to persisted one
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a003' with method 'GET'
    Then  I expect status code is 200
    And   I store 'validityPeriodStart' as '{{response.body.validityPeriod.start}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a003/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        },
        "suspensionPeriod": {
          "start": "2099-05-30T00:00:00Z",
          "end": "2099-06-01T00:00:00Z"
        },
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.validity_start_frozen'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 611 - Should return 400 when new validity start is in the past
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-611",
        "lastname": "Status",
        "firstname": "PastStart",
        "email": "status611@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2020-01-01T00:00:00Z",
          "end": "2099-01-01T00:00:00Z"
        },
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

  Scenario: 612 - Should return 400 when validity end is in the past
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a003' with method 'GET'
    Then  I expect status code is 200
    And   I store 'validityPeriodStart' as '{{response.body.validityPeriod.start}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a003/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "{{ctx.validityPeriodStart}}",
          "end": "2025-01-01T00:00:00Z"
        },
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

  Scenario: 613 - Should return 400 when suspension start is before validity start
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-613",
        "lastname": "Status",
        "firstname": "SuspensionBeforeValidity",
        "email": "status613@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-06-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        },
        "suspensionPeriod": {
          "start": "2099-03-01T00:00:00Z",
          "end": "2099-03-15T00:00:00Z"
        },
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

  Scenario: 614 - Should return 400 when suspension start is in the past
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a003' with method 'GET'
    Then  I expect status code is 200
    And   I store 'validityPeriodStart' as '{{response.body.validityPeriod.start}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a003/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "{{ctx.validityPeriodStart}}",
          "end": "2099-12-31T00:00:00Z"
        },
        "suspensionPeriod": {
          "start": "2025-01-01T00:00:00Z",
          "end": "2025-01-15T00:00:00Z"
        },
        "activationAt": null,
        "statusReason": null,
        "statusSubreason": null,
        "statusComment": null
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.suspension_start_in_past'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 615 - Should return 400 when suspension is outside validity end
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-615",
        "lastname": "Status",
        "firstname": "SuspensionAfterEnd",
        "email": "status615@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-06-30T00:00:00Z"
        },
        "suspensionPeriod": {
          "start": "2099-08-01T00:00:00Z",
          "end": "2099-08-15T00:00:00Z"
        },
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

  Scenario: 616 - Should accept open-ended validity (end null) without suspension
    When  I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-616",
        "lastname": "Status",
        "firstname": "OpenEnded",
        "email": "status616@example.com",
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": "2099-12-31T00:00:00Z"
        }
      }
      """
    Then  I expect status code is 201
    And   I store 'accountId' as '{{response.body.id}}' in context

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}/status' with method 'PUT' with body:
      """
      {
        "validityPeriod": {
          "start": "2099-01-01T00:00:00Z",
          "end": null
        },
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

  Scenario: 701 - Should activate account when business rules are satisfied
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a002/status/activation' with method 'PUT' with body:
      """
      {
        "activationAt": "2025-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 200
    And   I expect '{{response.body.activationAt}}' is not empty
    And   I expect '{{response.body.status}}' is 'ACTIVE'

  Scenario: 702 - Should return 404 when no account status row exists yet
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a001/status/activation' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.not_found'
    And   I expect '{{response.body.status}}' is '404'

  Scenario: 703 - Should return 400 when account is already activated
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a002/status/activation' with method 'PUT' with body:
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
        }
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
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.validity_in_future'
    And   I expect '{{response.body.status}}' is '400'

    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario: 705 - Should return 400 when activationAt is before validity start
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a003/status/activation' with method 'PUT' with body:
      """
      {
        "activationAt": "2020-01-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.before_validity_start'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 706 - Should return 400 when activationAt is in the future
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-00000000a004/status/activation' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 400
    And   I expect '{{response.body.errorKey}}' is 'error.account.status.activation.in_future'
    And   I expect '{{response.body.status}}' is '400'

  Scenario: 707 - Should return 404 when activating unknown account
    When  I request '{{env.E2E_API_URL}}/accounts/00000000-0000-0000-0000-000000000000/status/activation' with method 'PUT' with body:
      """
      {
        "activationAt": "2099-06-01T00:00:00Z"
      }
      """
    Then  I expect status code is 404
    And   I expect '{{response.body.errorKey}}' is 'error.account.not_found'
    And   I expect '{{response.body.status}}' is '404'
