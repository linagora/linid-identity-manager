Feature: Test Frontend Users Module

  ################## Navigation ##################
  ## 101 Should load frontend successfully

  Scenario: 101 - Should load frontend successfully
    Given I visit the '{{env.E2E_FRONT_URL}}'
    Then  I expect current url is '{{env.E2E_FRONT_URL}}/'
