Feature: Test Module Users Edit and Create

  ################## User Creation ##################
  ## 101 Should navigate to create new user page
  ## 102 Should dislay correct form sections and fields on creation page
  ## 103 Should successfully create new user with valid data
  ## 104 Should successfully display all data in details vue
  ## 105 Should display newly created user in users table

  ################## User Edit ##################
  ## 201 Should redirect to user detail page when clicking View
  ## 202 Should display all user information on detail page
  ## 203 Should redirect to edit page
  ## 204 Should display correct form sections and fields on edit page
  ## 205 Should pre-fill form fields with existing user data
  ## 206 Should enabled save button on selecting a date in datepicker
  ## 207 Should successfully update user data and reflect changes in detail page
  ## 208 Should reflect updated user data in users table
  ## 209 Remove the user

  Scenario: Complete user editing and creation workflow
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/moduleUsers'

    ####################################################
    ################## User Creation ##################
    ####################################################

    ## 101 Should navigate to create new user page
    When I click on '[data-cy="button_create"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/new"
    And I expect the HTML element '[data-cy="new-user-page"]' to be visible
    And I expect the HTML element '[data-cy="title"]' contains "Créer un nouvel utilisateur"

    ## 102 Should dislay correct form sections and fields on creation page
    And I expect the HTML element '[data-cy="form-section-card_main"]' to be visible
    And I expect the HTML element '[data-cy="form-section-title_main"]' contains "Formulaire de création de l'utilisateur"
    And I expect the HTML element '[data-cy="form-section-description_main"]' contains "Entrez les informations de base de l'utilisateur"
    And I expect the HTML element '[data-cy="form-section-card_secondary"]' to be visible
    And I expect the HTML element '[data-cy="form-section-title_secondary"]' contains "Informations supplémentaires"
    And I expect the HTML element '[data-cy="form-section-description_secondary"]' contains "Des détails supplémentaires sur l'utilisateur peuvent être ajoutés ici."
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_email"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_firstName"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_lastName"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_displayName"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field_role"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field_enabled"]' to be visible

    ## 103 Should successfully create new user with valid data
    When I set the text "michael.scott@gmail.com" in the HTML element '[data-cy="field_email"]'
    Then I set the text "Michael" in the HTML element '[data-cy="field_firstName"]'
    And I set the text "Scott" in the HTML element '[data-cy="field_lastName"]'
    And I set the text "Admin" in the HTML element '[data-cy="field_role"]'
    And I set the text "1985/03/10" in the HTML element '[data-cy="field_dateOfBirth"]'
    And I click on '[data-cy="button_confirm"]'

    ## 104 Should successfully display all data in details vue
    And I expect the HTML element '[data-cy="user-details-page_title"]' contains "Détails de l'utilisateur"
    And I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "michael.scott@gmail.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Michael"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Scott"
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "Admin"
    And I expect the HTML element '[data-cy="information-card--dateOfBirth"] [data-cy="value"]' contains "1985/03/10"

    ## 105 Should display newly created user in users table
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers"
    And I expect the HTML element '[data-cy="user-row"]' appear 7 times on screen
    And I expect the HTML element '[data-cy="cell-email_00000000-0000-0000-0000-000000000007"]' contains "michael.scott@gmail.com"
    And I expect the HTML element '[data-cy="cell-firstName_00000000-0000-0000-0000-000000000007"]' contains "Michael"
    And I expect the HTML element '[data-cy="cell-lastName_00000000-0000-0000-0000-000000000007"]' contains "Scott"

    ####################################################
    ################## User Edit ######################
    ####################################################

    ## 201 Should redirect to user detail page when clicking View
    When I click on '[data-cy="see-button_00000000-0000-0000-0000-000000000007"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="user-details-page_title"]' contains "Détails de l'utilisateur"

    ## 202 Should display all user information on detail page
    And I expect the HTML element '[data-cy="user-details-card"]' to be visible
    And I expect the HTML element '[data-cy="information-card--id"] [data-cy="value"]' contains "00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "michael.scott@gmail.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Michael"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Scott"
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "Admin"
    And I expect the HTML element '[data-cy="information-card--dateOfBirth"] [data-cy="value"]' contains "1985/03/10"

    ## 203 Should redirect to edit page
    And I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    And I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007/edit"
    And I expect the HTML element '[data-cy="title"]' contains "Édition de l'utilisateur"

    ## 204 Should display correct form sections and fields on edit page
    And I expect the HTML element '[data-cy="form-section-card_main"]' to be visible
    And I expect the HTML element '[data-cy="form-section-title_main"]' contains "Formulaire d'édition de l'utilisateur"
    And I expect the HTML element '[data-cy="form-section-description_main"]' contains "Modifiez les informations de l'utilisateur et cliquez sur 'Enregistrer' pour appliquer les modifications."
    And I expect the HTML element '[data-cy="form-section-card_secondary"]' to be visible
    And I expect the HTML element '[data-cy="form-section-title_secondary"]' contains "Informations secondaires"
    And I expect the HTML element '[data-cy="form-section-description_secondary"]' contains "Des détails supplémentaires sur l'utilisateur peuvent être modifiés ici."
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_email"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_firstName"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_lastName"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_main"] [data-cy="field_displayName"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field_role"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field_enabled"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field_dateOfBirth"]' to be visible

    ## 205 Should pre-fill form fields with existing user data
    And I expect the HTML element '[data-cy="field_email"]' to have value "michael.scott@gmail.com"
    And I expect the HTML element '[data-cy="field_firstName"]' to have value "Michael"
    And I expect the HTML element '[data-cy="field_lastName"]' to have value "Scott"
    And I expect the HTML element '[data-cy="field_role"]' to have value "Admin"
    And I expect the HTML element '[data-cy="field_dateOfBirth"]' to have value "1985/03/10"

    ## 206 Should enabled save button on selecting a date in datepicker
    And I expect the HTML element '[data-cy="button_confirm"]' to be disabled
    When I click on '[data-cy="field-container_dateOfBirth"] .q-icon'
    And I click on '.q-date__calendar-item--in:nth(1)'
    Then I expect the HTML element '[data-cy="button_confirm"]' to be enabled

    ## 207 Should successfully update user data and reflect changes in detail page
    When I set the text "Jean.dupont@example.com" in the HTML element '[data-cy="field_email"]'
    And I set the text "Jean" in the HTML element '[data-cy="field_firstName"]'
    And I set the text "Dupont" in the HTML element '[data-cy="field_lastName"]'
    And I set the text "User" in the HTML element '[data-cy="field_role"]'
    And I click on '[data-cy="button_confirm"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "Jean.dupont@example.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Jean"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Dupont"
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "User"

    ## 208 Should reflect updated user data in users table
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers"
    And I expect the HTML element '[data-cy="cell-email_00000000-0000-0000-0000-000000000007"]' contains "Jean.dupont@example.com"
    And I expect the HTML element '[data-cy="cell-firstName_00000000-0000-0000-0000-000000000007"]' contains "Jean"
    And I expect the HTML element '[data-cy="cell-lastName_00000000-0000-0000-0000-000000000007"]' contains "Dupont"

    ## 209 Remove the user
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000007' with method 'DELETE'
    Then I expect status code is 204
