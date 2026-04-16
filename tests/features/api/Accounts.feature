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

    When  I request '{{env.E2E_API_URL}}/accounts' with method 'GET'
    Then  I expect status code is 200
    And   I expect '{{response.body.totalElements}}' is not empty
    And   I expect '{{response.body | json}}' contains 'ext-301'

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
