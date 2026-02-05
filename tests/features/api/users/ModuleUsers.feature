Feature: Test API Users Module

  # Note: These tests run against linid-im-api which is configured to use mock-api as backend.
  # Tests are designed to be resilient to state variations where possible.

  ################## Metadata ########################
  ## 101 Should return users entity metadata with name and attributes

  ################## Find All (GET /api/users) #######
  ## 201 Should return paginated list of users
  ## 202 Should return 405 for unsupported method on users collection

  ################## Find By Id (GET /api/users/{id})
  ## 301 Should return 200 for existing user
  ## 302 Should return 404 for unknown user id

  ################## Create (POST /api/users) ########
  ## 401 Should return 400 for invalid JSON body on POST
  ## 402 Should create a new user with all required fields
  ## 403 Should return 400 when creating user without email
  ## 404 Should return 400 when creating user without firstName
  ## 405 Should return 400 when creating user without lastName
  ## 406 Should return 400 when creating user with invalid email format

  ################## Update (PUT /api/users/{id}) ####
  ## 501 Should return 200 when updating existing user
  ## 502 Should return 400 for invalid JSON body on PUT
  ## 503 Should return 404 when updating non-existent user
  ## 504 Should return 400 when updating user without email
  ## 505 Should return 400 when updating user without firstName
  ## 506 Should return 400 when updating user without lastName
  ## 507 Should return 400 when updating user with invalid email format

  ################## Partial Update (PATCH /api/users/{id})
  ## 601 Should return 200 when patching existing user
  ## 602 Should return 400 for invalid JSON body on PATCH
  ## 603 Should return 404 when patching non-existent user

  ################## Delete (DELETE /api/users/{id})
  ## 701 Should return 204 when deleting existing user
  ## 702 Should return 404 when deleting non-existent user

  ################## Validate (POST /api/users/validate/{attribute})
  ## 801 Should return 204 when validating attribute with valid value
  ## 802 Should return 400 when validating attribute with empty value
  ## 803 Should return 400 when validating attribute with null value
  ## 804 Should return 404 when validating unknown attribute
  ## 805 Should return 400 when validating email with invalid format

  ####################################################
  ################## Metadata ########################
  ####################################################

  Scenario: 101 - Should return users entity metadata with name and attributes
    When I request '{{env.E2E_API_URL}}/metadata/entities/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.name}}' is 'user'
    And  I expect '{{response.body.attributes.length}}' is '6'
    And  I expect '{{response.body.attributes[0].name}}' is 'id'
    And  I expect '{{response.body.attributes[1].name}}' is 'email'
    And  I expect '{{response.body.attributes[2].name}}' is 'firstName'
    And  I expect '{{response.body.attributes[3].name}}' is 'lastName'
    And  I expect '{{response.body.attributes[4].name}}' is 'displayName'
    And  I expect '{{response.body.attributes[5].name}}' is 'enabled'

  ####################################################
  ################## Find All (GET /api/users) #######
  ####################################################

  # Note: API returns 206 Partial Content when totalPages > 1
  Scenario: 201 - Should return paginated list of users
    When I request '{{env.E2E_API_URL}}/api/users?size=100' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.pageable.pageNumber}}' is '0'

  Scenario Outline: 202 - Should return 405 for unsupported method "<method>" on users collection
    When I request '{{env.E2E_API_URL}}/api/users' with method '<method>'
    Then I expect status code is 405

    Examples:
      | method |
      | PUT    |
      | PATCH  |
      | DELETE |

  ####################################################
  ################## Find By Id (GET /api/users/{id})
  ####################################################

  Scenario: 301 - Should return 200 for existing user
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000001'
    And  I expect '{{response.body.email}}' is not empty

  Scenario: 302 - Should return 404 for unknown user id
    When I request '{{env.E2E_API_URL}}/api/users/unknown-id-that-does-not-exist' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'
    And  I expect '{{response.body.errorKey}}' is 'hpp.error404'

  ####################################################
  ################## Create (POST /api/users) ########
  ####################################################

  Scenario: 401 - Should return 400 for invalid JSON body on POST
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 402 - Should create a new user with all required fields
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"email": "test.create@example.com", "firstName": "Test", "lastName": "Create"}
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.email}}' is 'test.create@example.com'
    And  I expect '{{response.body.firstName}}' is 'Test'
    And  I expect '{{response.body.lastName}}' is 'Create'

  Scenario: 403 - Should return 400 when creating user without email
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"firstName": "Test", "lastName": "Create"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '2'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'email'
    And  I expect '{{response.body.errors[1].key}}' is 'error.plugin.regexValidation.invalid.value'
    And  I expect '{{response.body.errors[1].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[1].context.attribute}}' is 'email'

  Scenario: 404 - Should return 400 when creating user without firstName
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"email": "test@example.com", "lastName": "Create"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '1'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'firstName'

  Scenario: 405 - Should return 400 when creating user without lastName
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"email": "test@example.com", "firstName": "Test"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '1'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'lastName'

  Scenario: 406 - Should return 400 when creating user with invalid email format
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"email": "notanemail", "firstName": "Test", "lastName": "Create"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '1'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.regexValidation.invalid.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'email'

  ####################################################
  ################## Update (PUT /api/users/{id}) ####
  ####################################################

  Scenario: 501 - Should return 200 when updating existing user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'PUT' with body:
      """
      {"email": "john.updated@example.com", "firstName": "John", "lastName": "Updated"}
      """
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000001'
    And  I expect '{{response.body.email}}' is 'john.updated@example.com'

  Scenario: 502 - Should return 400 for invalid JSON body on PUT
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/any-user-id' with method 'PUT' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 503 - Should return 404 when updating non-existent user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/nonexistent-user-id-12345' with method 'PUT' with body:
      """
      {"email": "test@example.com", "firstName": "Test", "lastName": "User"}
      """
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'
    And  I expect '{{response.body.errorKey}}' is 'hpp.error404'

  Scenario: 504 - Should return 400 when updating user without email
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/any-user-id' with method 'PUT' with body:
      """
      {"firstName": "Test", "lastName": "User"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '2'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'email'
    And  I expect '{{response.body.errors[1].key}}' is 'error.plugin.regexValidation.invalid.value'
    And  I expect '{{response.body.errors[1].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[1].context.attribute}}' is 'email'

  Scenario: 505 - Should return 400 when updating user without firstName
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/any-user-id' with method 'PUT' with body:
      """
      {"email": "test@example.com", "lastName": "User"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '1'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'firstName'

  Scenario: 506 - Should return 400 when updating user without lastName
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/any-user-id' with method 'PUT' with body:
      """
      {"email": "test@example.com", "firstName": "Test"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '1'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'lastName'

  Scenario: 507 - Should return 400 when updating user with invalid email format
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/any-user-id' with method 'PUT' with body:
      """
      {"email": "notanemail", "firstName": "Test", "lastName": "User"}
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.error}}' is 'Validation errors occurred for entity: user'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '1'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.regexValidation.invalid.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'email'

  ####################################################
  ################## Partial Update (PATCH /api/users/{id})
  ####################################################

  Scenario: 601 - Should return 200 when patching existing user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000002' with method 'PATCH' with body:
      """
      {"displayName": "Jane Updated"}
      """
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000002'
    And  I expect '{{response.body.displayName}}' is 'Jane Updated'

  Scenario: 602 - Should return 400 for invalid JSON body on PATCH
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/any-user-id' with method 'PATCH' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 603 - Should return 404 when patching non-existent user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/nonexistent-user-id-67890' with method 'PATCH' with body:
      """
      {"displayName": "Test"}
      """
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'
    And  I expect '{{response.body.errorKey}}' is 'hpp.error404'

  ####################################################
  ################## Delete (DELETE /api/users/{id})
  ####################################################

  Scenario: 701 - Should return 204 when deleting existing user
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 702 - Should return 404 when deleting non-existent user
    When I request '{{env.E2E_API_URL}}/api/users/nonexistent-user-id-99999' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'
    And  I expect '{{response.body.errorKey}}' is 'hpp.error404'

  ####################################################
  ################## Validate (POST /api/users/validate/{attribute})
  ####################################################

  Scenario Outline: 801 - Should return 204 when validating "<attribute>" with valid value "<value>"
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/validate/<attribute>' with method 'POST' with body:
      """
      "<value>"
      """
    Then I expect status code is 204

    Examples:
      | attribute | value            |
      | email     | test@example.com |
      | firstName | John             |
      | lastName  | Doe              |

  Scenario Outline: 802 - Should return 400 when validating "<attribute>" with empty value
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/validate/<attribute>' with method 'POST' with body:
      """
      ""
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is '<attribute>'

    Examples:
      | attribute |
      | email     |
      | firstName |
      | lastName  |

  Scenario Outline: 803 - Should return 400 when validating "<attribute>" with null value
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/validate/<attribute>' with method 'POST'
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.required.empty.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is '<attribute>'

    Examples:
      | attribute |
      | email     |
      | firstName |
      | lastName  |

  Scenario: 804 - Should return 404 when validating unknown attribute
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/validate/unknownAttribute' with method 'POST' with body:
      """
      "test"
      """
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'
    And  I expect '{{response.body.errorKey}}' is 'error.attribute.unknown'

  Scenario Outline: 805 - Should return 400 when validating email with invalid format "<value>"
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/validate/email' with method 'POST' with body:
      """
      "<value>"
      """
    Then I expect status code is 400
    And  I expect '{{response.body.status}}' is '400'
    And  I expect '{{response.body.errorKey}}' is 'error.entity.attributes'
    And  I expect '{{response.body.errors.length}}' is '1'
    And  I expect '{{response.body.errors[0].key}}' is 'error.plugin.regexValidation.invalid.value'
    And  I expect '{{response.body.errors[0].context.entity}}' is 'user'
    And  I expect '{{response.body.errors[0].context.attribute}}' is 'email'

    Examples:
      | value              |
      | notanemail         |
      | test@example.org   |
      | @example.com       |
