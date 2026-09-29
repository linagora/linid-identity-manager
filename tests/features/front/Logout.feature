Feature: Test logout

  ################## Profile Menu ##################
  ## 101 Should display the logout entry in the profile menu
  ## 102 Should open a confirmation dialog and stay in the application on cancel
  ## 103 Should send the user to the SSO logout confirmation page on confirm and get back into the application on refuse

  ################## SSO Logout ##################
  ## 201 Should sign the user back in when visiting the logged-out page with an active SSO session
  ## 202 Should end the SSO session and ask for credentials again when the logout is accepted
  ## 203 Should ask for credentials when visiting the logged-out page without an SSO session

  Scenario: Roundtrip about logout
    Given I set the viewport size to 1920 px by 1080 px
    And   I visit the '{{ env.E2E_FRONT_URL }}/'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then  I expect the HTML element '[data-cy="home-page"]' to be visible

  ####################################################
  ################## Profile Menu ####################
  ####################################################

  ## 101 Should display the logout entry in the profile menu
    When I click on '[data-cy="header_profile_button"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' to be visible
    And  I expect the HTML element '[data-cy="header_profile_logout"]' to be visible
    And  I expect the HTML element '[data-cy="header_profile_logout_label"]' contains "Se déconnecter"

  ## 102 Should open a confirmation dialog and stay in the application on cancel
    When I click on '[data-cy="header_profile_logout"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' not exists
    And  I expect the HTML element '[data-cy="confirmation_dialog_card"]' to be visible
    And  I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Déconnexion"
    And  I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Êtes-vous sûr de vouloir vous déconnecter ?"
    And  I expect the HTML element '[data-cy="button_cancel"]' contains "Annuler"
    And  I expect the HTML element '[data-cy="button_confirm"]' contains "Se déconnecter"
    When I click on '[data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog_card"]' not exists
    And  I expect the HTML element '[data-cy="home-page"]' to be visible

  ## 103 Should send the user to the SSO logout confirmation page on confirm and get back into the application on refuse
    When I click on '[data-cy="header_profile_button"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' to be visible
    When I click on '[data-cy="header_profile_logout"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog_card"]' to be visible
    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element 'form.confirm' to be visible
    And  I expect the HTML element 'form.confirm button.positive' to be visible
    And  I expect the HTML element 'form.confirm #refuse' to be visible
    And  I expect the HTML element '[data-cy="home-page"]' not exists
    When I click on 'form.confirm #refuse'
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    And  I expect the HTML element 'input#userfield' not exists

  ####################################################
  ################## SSO Logout ######################
  ####################################################

  ## 201 Should sign the user back in when visiting the logged-out page with an active SSO session
    When I visit the '{{ env.E2E_FRONT_URL }}/logged-out'
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    And  I expect the HTML element 'input#userfield' not exists

  ## 202 Should end the SSO session and ask for credentials again when the logout is accepted
    When I click on '[data-cy="header_profile_button"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' to be visible
    When I click on '[data-cy="header_profile_logout"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog_card"]' to be visible
    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element 'form.confirm' to be visible
    When I click on 'form.confirm button.positive'
    Then I expect the HTML element 'input#userfield' to be visible
    And  I expect the HTML element '[data-cy="home-page"]' not exists

  ## 203 Should ask for credentials when visiting the logged-out page without an SSO session
    When I visit the '{{ env.E2E_FRONT_URL }}/logged-out'
    Then I expect the HTML element 'input#userfield' to be visible
    And  I expect the HTML element '[data-cy="home-page"]' not exists
