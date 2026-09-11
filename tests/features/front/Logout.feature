Feature: Test logout

  ################## Profile Menu ##################
  ## 101 Should display the logout entry in the profile menu
  ## 102 Should open a confirmation dialog and stay in the application on cancel
  ## 103 Should log out on confirm and ask for credentials again

  ################## Logout URL ##################
  ## 201 Should end the LemonLDAP session and ask for credentials again when visiting the logout URL

  Scenario: Roundtrip about logout
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then I expect the HTML element '[data-cy="home-page"]' to be visible

  ####################################################
  ################## Profile Menu ####################
  ####################################################

  ## 101 Should display the logout entry in the profile menu
    When I click on '[data-cy="header_profile_button"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' to be visible
    And I expect the HTML element '[data-cy="header_profile_logout"]' to be visible
    And I expect the HTML element '[data-cy="header_profile_logout_label"]' contains "Se déconnecter"

  ## 102 Should open a confirmation dialog and stay in the application on cancel
    When I click on '[data-cy="header_profile_logout"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' not exists
    And I expect the HTML element '[data-cy="confirmation_dialog_card"]' to be visible
    And I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Déconnexion"
    And I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Êtes-vous sûr de vouloir vous déconnecter ?"
    And I expect the HTML element '[data-cy="button_cancel"]' contains "Annuler"
    And I expect the HTML element '[data-cy="button_confirm"]' contains "Se déconnecter"
    When I click on '[data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog_card"]' not exists
    And I expect the HTML element '[data-cy="home-page"]' to be visible

  ## 103 Should log out on confirm and ask for credentials again
    When I click on '[data-cy="header_profile_button"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' to be visible
    When I click on '[data-cy="header_profile_logout"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog_card"]' to be visible
    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element 'input#userfield' to be visible
    And I expect the HTML element '[data-cy="home-page"]' not exists

  ####################################################
  ################## Logout URL ######################
  ####################################################

  ## 201 Should end the LemonLDAP session and ask for credentials again when visiting the logout URL
    When I set the text "admin" in the HTML element "input#userfield"
    And I set the text "password" in the HTML element "input#passwordfield"
    And I click on "button.btn-success"
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    When I visit the '{{ env.E2E_FRONT_URL }}/logout'
    Then I expect the HTML element 'input#userfield' to be visible
    And I expect the HTML element '[data-cy="home-page"]' not exists
