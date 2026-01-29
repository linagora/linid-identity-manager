Feature: Test API Users Module

  ################## Metadata ########################
  ## 101 Should return users entity metadata with name and attributes

  ################## Find All (GET /api/users) #######
  ## 201 Should return paginated list of users with content
  ## 202 Should return 405 for unsupported method on users collection

  ################## Find By Id (GET /api/users/{id})
  ## 301 Should return a user by id with all properties
  ## 302 Should return 404 for unknown user id

  ################## Create (POST /api/users) ########
  ## 401 Should return 400 for invalid JSON body on POST
  ## 402 Should create a new user with all required fields

  ################## Update (PUT /api/users/{id}) ####
  ## 501 Should return 400 for invalid JSON body on PUT
  ## 502 Should update an existing user with all fields
  ## 503 Should return 404 when updating non-existent user
  ## 505 Should return 405 for unsupported method on user resource

  ################## Partial Update (PATCH /api/users/{id})
  ## 601 Should return 400 for invalid JSON body on PATCH
  ## 602 Should patch an existing user with partial data
  ## 603 Should return 404 when patching non-existent user

  ################## Delete (DELETE /api/users/{id})
  ## 701 Should delete an existing user
  ## 702 Should return 404 when deleting non-existent user

  ####################################################
  ################## Metadata ########################
  ####################################################

  Scenario: Should return users entity metadata with name and attributes
    When I request '{{env.E2E_API_URL}}/metadata/entities/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.name}}' is 'user'
    And  I expect '{{response.body.attributes}}' is not empty

  ####################################################
  ################## Find All (GET /api/users) #######
  ####################################################

  Scenario: Should return paginated list of users with content
    When I request '{{env.E2E_API_URL}}/api/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.pageable.pageNumber}}' is '0'
    And  I expect '{{response.body.totalElements}}' is not empty

  Scenario Outline: Should return 405 for unsupported method "<method>" on users collection
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

  Scenario: Should return a user by id with all properties
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000002' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000002'
    And  I expect '{{response.body.email}}' is 'jane.roe@example.com'
    And  I expect '{{response.body.firstName}}' is 'Jane'
    And  I expect '{{response.body.lastName}}' is 'Roe'

  Scenario: Should return 404 for unknown user id
    When I request '{{env.E2E_API_URL}}/api/users/unknown-id' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'

  ####################################################
  ################## Create (POST /api/users) ########
  ####################################################

  Scenario: Should return 400 for invalid JSON body on POST
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: Should create a new user with all required fields
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"email": "new.user@example.com", "firstName": "New", "lastName": "User"}
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.email}}' is 'new.user@example.com'
    And  I expect '{{response.body.firstName}}' is 'New'
    And  I expect '{{response.body.lastName}}' is 'User'

  # NOTE: Validation of required fields is not yet implemented in the mock-api
  # These tests are commented out until validation is added
  # Scenario: 403 - Should return 400 when creating user without email
  # Scenario: 404 - Should return 400 when creating user without firstName
  # Scenario: 405 - Should return 400 when creating user without lastName

  ####################################################
  ################## Update (PUT /api/users/{id}) ####
  ####################################################

  Scenario: Should return 400 for invalid JSON body on PUT
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/test-id' with method 'PUT' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: Should update an existing user with all fields
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'PUT' with body:
      """
      {"email": "john.updated@example.com", "firstName": "John", "lastName": "Updated"}
      """
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000001'
    And  I expect '{{response.body.email}}' is 'john.updated@example.com'
    And  I expect '{{response.body.firstName}}' is 'John'
    And  I expect '{{response.body.lastName}}' is 'Updated'

  Scenario: Should return 404 when updating non-existent user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/nonexistent-id' with method 'PUT' with body:
      """
      {"email": "test@example.com", "firstName": "Test", "lastName": "User"}
      """
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'

  # NOTE: Validation of required fields is not yet implemented
  # Scenario: 504 - Should return 400 when updating user without email

  Scenario Outline: Should return 405 for unsupported method "<method>" on user resource
    When I request '{{env.E2E_API_URL}}/api/users/test-id' with method '<method>'
    Then I expect status code is 405

    Examples:
      | method |
      | POST   |

  ####################################################
  ################## Partial Update (PATCH /api/users/{id})
  ####################################################

  Scenario: Should return 400 for invalid JSON body on PATCH
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/test-id' with method 'PATCH' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: Should patch an existing user with partial data
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'PATCH' with body:
      """
      {"displayName": "Johnny D."}
      """
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000001'
    And  I expect '{{response.body.displayName}}' is 'Johnny D.'

  Scenario: Should return 404 when patching non-existent user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/nonexistent-id' with method 'PATCH' with body:
      """
      {"displayName": "Test"}
      """
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'

  ####################################################
  ################## Delete (DELETE /api/users/{id})
  ####################################################

  Scenario: Should delete an existing user
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000002' with method 'DELETE'
    Then I expect status code is 204

  Scenario: Should return 404 when deleting non-existent user
    When I request '{{env.E2E_API_URL}}/api/users/nonexistent-id' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.status}}' is '404'
