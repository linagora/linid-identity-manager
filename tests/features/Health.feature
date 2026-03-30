Feature: Test UI Health Check

  ################## Health ##########################
  ## 100 Application homepage is accessible

  ####################################################
  ################## Health ##########################
  ####################################################

  Scenario: 100 - Application homepage is accessible
    Given I visit the "{{ env.E2E_FRONT_URL }}/"
    Then I expect the HTML element "[data-cy='home-page']" exists
