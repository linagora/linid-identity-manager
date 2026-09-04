Feature: Test logout

  ################## RP-initiated logout ##################
  ## 101 Should end the LemonLDAP session and ask for credentials again

  Scenario: Roundtrip about logout
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then I expect the HTML element '[data-cy="home-page"]' to be visible

  ####################################################
  ################## RP-initiated logout #############
  ####################################################

  ## 101 Should end the LemonLDAP session and ask for credentials again
    When I visit the '{{ env.E2E_FRONT_URL }}/logout'
    Then I expect the HTML element 'input#userfield' to be visible
    And I expect the HTML element '[data-cy="home-page"]' not exists
