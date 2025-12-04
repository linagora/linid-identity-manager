Feature: Test API Metadata Endpoints

  ################## Routes ##################
  ## 101 Should return all available routes
  ## 102 Should return 405 for invalid HTTP method on routes

  ################## Entities ##################
  ## 201 Should return all entities metadata
  ## 202 Should return user entity metadata with name
  ## 203 Should return user entity metadata with attributes
  ## 204 Should return 404 for non-existent entity
  ## 205 Should return 405 for invalid HTTP method on entities

  Scenario: 101 - Should return all available routes
    When I request '{{env.E2E_API_URL}}/metadata/routes' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario: 102 - Should return 405 for invalid HTTP method on routes
    When I request '{{env.E2E_API_URL}}/metadata/routes' with method 'DELETE'
    Then I expect status code is 405

  Scenario: 201 - Should return all entities metadata
    When I request '{{env.E2E_API_URL}}/metadata/entities' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario: 202 - Should return user entity metadata with name
    When I request '{{env.E2E_API_URL}}/metadata/entities/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.name}}' is 'user'

  Scenario: 203 - Should return user entity metadata with attributes
    When I request '{{env.E2E_API_URL}}/metadata/entities/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.attributes}}' is not empty

  Scenario: 204 - Should return 404 for non-existent entity
    When I request '{{env.E2E_API_URL}}/metadata/entities/nonexistent' with method 'GET'
    Then I expect status code is 404

  Scenario: 205 - Should return 405 for invalid HTTP method on entities
    When I request '{{env.E2E_API_URL}}/metadata/entities' with method 'POST'
    Then I expect status code is 405
