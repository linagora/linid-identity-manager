Feature: Test Frontend Homepage

  ################## Homepage ##################
  ## 101 Should load the homepage successfully

  Scenario: 101 - Should load the homepage successfully
    Given I visit the '{{env.E2E_FRONT_URL}}'
    Then  I expect current url is '{{env.E2E_FRONT_URL}}/'
