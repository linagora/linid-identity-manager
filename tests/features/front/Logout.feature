Feature: Test logout

  ################## RP-initiated logout ##################
  ## 101 Should ask for a logout confirmation on the SSO portal when visiting the logout URL
  ## 102 Should keep the SSO session and get back into the application when the logout is refused
  ## 103 Should sign the user back in when visiting the logged-out page with an active SSO session
  ## 104 Should end the SSO session and ask for credentials again when the logout is accepted
  ## 105 Should ask for credentials when visiting the logged-out page without an SSO session

  Scenario: Roundtrip about logout
    Given I set the viewport size to 1920 px by 1080 px
    And   I visit the '{{ env.E2E_FRONT_URL }}/'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then  I expect the HTML element '[data-cy="home-page"]' to be visible

  ####################################################
  ################## RP-initiated logout #############
  ####################################################

  ## 101 Should ask for a logout confirmation on the SSO portal when visiting the logout URL
    When I visit the '{{ env.E2E_FRONT_URL }}/logout'
    Then I expect the HTML element 'form.confirm' to be visible
    And  I expect the HTML element 'form.confirm button.positive' to be visible
    And  I expect the HTML element 'form.confirm #refuse' to be visible

  ## 102 Should keep the SSO session and get back into the application when the logout is refused
    When I click on 'form.confirm #refuse'
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    And  I expect the HTML element 'input#userfield' not exists

  ## 103 Should sign the user back in when visiting the logged-out page with an active SSO session
    When I visit the '{{ env.E2E_FRONT_URL }}/logged-out'
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    And  I expect the HTML element 'input#userfield' not exists

  ## 104 Should end the SSO session and ask for credentials again when the logout is accepted
    When I visit the '{{ env.E2E_FRONT_URL }}/logout'
    Then I expect the HTML element 'form.confirm' to be visible
    When I click on 'form.confirm button.positive'
    Then I expect the HTML element 'input#userfield' to be visible
    And  I expect the HTML element '[data-cy="home-page"]' not exists

  ## 105 Should ask for credentials when visiting the logged-out page without an SSO session
    When I visit the '{{ env.E2E_FRONT_URL }}/logged-out'
    Then I expect the HTML element 'input#userfield' to be visible
    And  I expect the HTML element '[data-cy="home-page"]' not exists
