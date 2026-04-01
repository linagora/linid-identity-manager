Feature: Test Copyright Filter

  ################## Copyright filter ##########################
  ## 101 Should include default copyright header on responses
  #############################################################

  Scenario: 101 - Should include default copyright header on actuator health
    When I request '{{env.E2E_API_URL}}/actuator/health' with method 'GET'
    Then I expect status code is 200
    And I expect http header 'X-Copyright' contains 'Linagora'
