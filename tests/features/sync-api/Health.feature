Feature: Test sync-api Health Check

  ################## Health ##########################
  ## 101 Should return UP status when sync-api is healthy

  ####################################################
  ################## Health ##########################
  ####################################################

  Scenario: 101 - Should return UP status when sync-api is healthy
    When I request '{{env.E2E_SYNC_API_URL}}/actuator/health' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.status}}' is 'UP'
