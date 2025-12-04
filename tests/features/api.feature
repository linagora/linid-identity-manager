Feature: Test API

    Scenario: Check if application is up
        When I request '{{env.E2E_API_URL}}/actuator/health' with method 'GET'
        Then I expect status code is 200
        And I log 'Response body: {{response.body | json}}'
        And I expect '{{response.body.status}}' is 'UP'
