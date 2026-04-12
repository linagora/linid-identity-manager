Feature: Test UI Health Check

  ################## Health ##########################
  ## 100 Application homepage is accessible

  ####################################################
  ################## Health ##########################
  ####################################################

  Scenario: 100 - Application homepage is accessible
    Given I visit the "{{ env.E2E_FRONT_URL }}/"
    Then I expect the HTML element "input#userfield" to be visible
    And I expect current url matches "^{{ env.E2E_AUTH_URL }}?.*$"
    When I set the text "admin" in the HTML element "input#userfield"
    And I set the text "password" in the HTML element "input#passwordfield"
    And I click on "button.btn-success"
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/"
    And I expect the HTML element "[data-cy='home-page']" exists
