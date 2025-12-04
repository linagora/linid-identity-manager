Feature: Test API Health Check

  ################## Health ##################
  ## 101 Should return UP status when application is healthy
  ## 102 Should return health components information
  ## 103 Should return 405 for unsupported method on health

  ################## Actuator ##################
  ## 201 Should return actuator links
  ## 202 Should return health link in actuator
  ## 203 Should return self link in actuator
  ## 204 Should return 405 for unsupported method on actuator

  Scenario: 101 - Should return UP status when application is healthy
    When I request '{{env.E2E_API_URL}}/actuator/health' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.status}}' is 'UP'

  Scenario: 102 - Should return health components information
    When I request '{{env.E2E_API_URL}}/actuator/health' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario: 103 - Should return 405 for unsupported method on health
    When I request '{{env.E2E_API_URL}}/actuator/health' with method 'DELETE'
    Then I expect status code is 405

  Scenario: 201 - Should return actuator links
    When I request '{{env.E2E_API_URL}}/actuator' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body._links}}' is not empty

  Scenario: 202 - Should return health link in actuator
    When I request '{{env.E2E_API_URL}}/actuator' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body._links.health}}' is not empty
    And  I expect '{{response.body._links.health.href}}' is not empty

  Scenario: 203 - Should return self link in actuator
    When I request '{{env.E2E_API_URL}}/actuator' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body._links.self}}' is not empty
    And  I expect '{{response.body._links.self.href}}' is not empty

  Scenario: 204 - Should return 405 for unsupported method on actuator
    When I request '{{env.E2E_API_URL}}/actuator' with method 'DELETE'
    Then I expect status code is 405
