Feature: Test API Metadata Endpoints

  ################## Routes ##########################
  ## 101 Should return all available routes with content
  ## 102 Should return 405 for unsupported method on routes

  ################## Entities ########################
  ## 201 Should return all entities metadata with content
  ## 202 Should return user entity metadata with name and attributes
  ## 203 Should return 404 for non-existent entity
  ## 204 Should return 405 for unsupported method on entities

  ####################################################
  ################## Routes ##########################
  ####################################################

  Scenario: Should return all available routes with content
    When I request '{{env.E2E_API_URL}}/metadata/routes' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario Outline: Should return 405 for unsupported method "<method>" on routes
    When I request '{{env.E2E_API_URL}}/metadata/routes' with method '<method>'
    Then I expect status code is 405

    Examples:
      | method |
      | POST   |
      | PUT    |
      | PATCH  |
      | DELETE |

  ####################################################
  ################## Entities ########################
  ####################################################

  Scenario: Should return all entities metadata with content
    When I request '{{env.E2E_API_URL}}/metadata/entities' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario: Should return user entity metadata with name and attributes
    When I request '{{env.E2E_API_URL}}/metadata/entities/users' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.name}}' is 'user'
    And  I expect '{{response.body.attributes}}' is not empty

  Scenario: Should return 404 for non-existent entity
    When I request '{{env.E2E_API_URL}}/metadata/entities/nonexistent' with method 'GET'
    Then I expect status code is 404
    And  I expect '{{response.body.error}}' is 'Unknown entity: nonexistent'
    And  I expect '{{response.body.status}}' is '404'

  Scenario Outline: Should return 405 for unsupported method "<method>" on entities
    When I request '{{env.E2E_API_URL}}/metadata/entities' with method '<method>'
    Then I expect status code is 405

    Examples:
      | method |
      | POST   |
      | PUT    |
      | PATCH  |
      | DELETE |
