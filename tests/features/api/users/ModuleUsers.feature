Feature: Test API Users Module

  ################## Metadata ##################
  ## 101 Should return user entity metadata with name
  ## 102 Should return user entity with attributes

  ################## Find All (GET /api/users) ##################
  ## 201 Should return paginated list of users
  ## 202 Should return 405 for unsupported method on users collection

  ################## Find By Id (GET /api/users/{id}) ##################
  ## 301 Should return a user by id
  ## 302 Should return 404 for unknown user id

  ################## Create (POST /api/users) ##################
  ## 401 Should return 400 for invalid JSON body on POST
  ## 402 Should create a new user
  ## 403 Should return 405 for unsupported method on collection

  ################## Update (PUT /api/users/{id}) ##################
  ## 501 Should return 400 for invalid JSON body on PUT
  ## 502 Should update an existing user
  ## 503 Should return 405 for unsupported method on user resource

  ################## Partial Update (PATCH /api/users/{id}) ##################
  ## 601 Should return 400 for invalid JSON body on PATCH
  ## 602 Should patch an existing user

  ################## Delete (DELETE /api/users/{id}) ##################
  ## 701 Should delete an existing user

  Scenario: 101 - Should return user entity metadata with name
    When I request '{{env.E2E_API_URL}}/metadata/entities/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.name}}' is 'user'

  Scenario: 102 - Should return user entity with attributes
    When I request '{{env.E2E_API_URL}}/metadata/entities/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.attributes}}' is not empty

  Scenario: 201 - Should return paginated list of users
    When I request '{{env.E2E_API_URL}}/api/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.pageable}}' is not empty
    And  I expect '{{response.body.totalElements}}' is not empty

  Scenario: 202 - Should return 405 for unsupported method on users collection
    When I request '{{env.E2E_API_URL}}/api/users' with method 'DELETE'
    Then I expect status code is 405

  Scenario: 301 - Should return a user by id
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000001'
    And  I expect '{{response.body.email}}' is not empty
    And  I expect '{{response.body.firstName}}' is not empty
    And  I expect '{{response.body.lastName}}' is not empty

  Scenario: 302 - Should return 404 for unknown user id
    When I request '{{env.E2E_API_URL}}/api/users/unknown-id' with method 'GET'
    Then I expect status code is 404

  Scenario: 401 - Should return 400 for invalid JSON body on POST
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 402 - Should create a new user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"email": "new.user@example.com", "firstName": "New", "lastName": "User"}
      """
    Then I expect status code is 201
    And  I expect '{{response.body.id}}' is not empty
    And  I expect '{{response.body.email}}' is 'new.user@example.com'
    And  I expect '{{response.body.firstName}}' is 'New'

  Scenario: 403 - Should return 405 for unsupported method on collection
    When I request '{{env.E2E_API_URL}}/api/users' with method 'PATCH'
    Then I expect status code is 405

  Scenario: 501 - Should return 400 for invalid JSON body on PUT
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/test-id' with method 'PUT' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 502 - Should update an existing user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'PUT' with body:
      """
      {"email": "john.updated@example.com", "firstName": "John", "lastName": "Updated"}
      """
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000001'
    And  I expect '{{response.body.email}}' is 'john.updated@example.com'

  Scenario: 503 - Should return 405 for unsupported method on user resource
    When I request '{{env.E2E_API_URL}}/api/users/test-id' with method 'POST'
    Then I expect status code is 405

  Scenario: 601 - Should return 400 for invalid JSON body on PATCH
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/test-id' with method 'PATCH' with body:
      """
      { invalid json }
      """
    Then I expect status code is 400
    And  I expect '{{response.body.error}}' is 'Bad Request'
    And  I expect '{{response.body.status}}' is '400'

  Scenario: 602 - Should patch an existing user
    Given I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000001' with method 'PATCH' with body:
      """
      {"displayName": "Johnny D."}
      """
    Then I expect status code is 200
    And  I expect '{{response.body.id}}' is '00000000-0000-0000-0000-000000000001'

  Scenario: 701 - Should delete an existing user
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000002' with method 'DELETE'
    Then I expect status code is 204
