Feature: Test Static List from Mock API

  ################## Static List Dropdown ##################
  ## 101 Should display role field as StaticList on creation page
  ## 102 Should populate dropdown with values from configuration
  ## 103 Should focus the option when there is a default value
  ## 104 Should not focus the option when there is an invalid default value
  ## 105 Should allow selecting a role value

  ################## Create User with Roles ##################
  ## 201 Should display required validation error message
  ## 202 Should successfully create user with selected role
  ## 203 Should display success notification after creation
  ## 204 Should redirect to detail page after creation
  ## 205 Should display selected role value on detail page

  ################## Edit User Role ##################
  ## 301 Should display current role on edit page
  ## 302 Should pre-fill role field with current value
  ## 303 Should allow changing role to a different value
  ## 304 Should display success notification after update
  ## 305 Should persist updated role on detail page

  ################## Cleanup ##################
  ## 401 Remove the test user

  Scenario: Complete static list workflow for role field
    Given I set the viewport size to 1920 px by 1080 px

    ####################################################
    ################## Static List Dropdown ############
    ####################################################

    ## 101 Should display roles fields as StaticList on creation page
    When I visit the '{{ env.E2E_FRONT_URL }}/moduleUsers/new'
    Then I expect the HTML element '[data-cy="new-user-page"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_role"]' to be visible
    And I expect the HTML element '[data-cy="field_role"] .q-select__focus-target' to have value ""
    And I expect the HTML element "[data-cy='field-container_role'] .q-field__label" contains "Rôle"
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_roleWithDefaultValue"]' to be visible
    And I expect the HTML element '[data-cy="field_roleWithDefaultValue"] .q-select__focus-target' to have value "admin"
    And I expect the HTML element "[data-cy='field-container_roleWithDefaultValue'] .q-field__label" contains "Rôle avec valeur par défaut"
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_roleWithInvalidDefaultValue"]' to be visible
    And I expect the HTML element '[data-cy="field_roleWithInvalidDefaultValue"] .q-select__focus-target' to have value ""
    And I expect the HTML element "[data-cy='field-container_roleWithInvalidDefaultValue'] .q-field__label" contains "Rôle avec valeur par défaut invalide *"

    ## 102 Should populate role dropdown with values from configuration
    When I click on '[data-cy="field_role"]'
    Then I expect the HTML element ".q-item" appear 3 times on screen
    And I expect the HTML element '.q-item:nth-child(1)' contains "user"
    And I expect the HTML element '.q-item:nth-child(2)' contains "admin"
    And I expect the HTML element '.q-item:nth-child(3)' contains "manager"
    And I expect the HTML element '.q-item.q-item--active' not exists

    ### 103 Should focus the option when there is a default value
    When I click on '[data-cy="field_roleWithDefaultValue"]'
    Then I expect the HTML element ".q-item" appear 3 times on screen
    And I expect the HTML element '.q-item:nth-child(2)' contains "admin"
    And I expect the HTML element '.q-item:nth-child(1).q-item--active' not exists
    And I expect the HTML element '.q-item:nth-child(2).q-item--active' exists
    And I expect the HTML element '.q-item:nth-child(3).q-item--active' not exists

    ## 104 Should not focus the option when there is an invalid default value
    When I click on '[data-cy="field_roleWithInvalidDefaultValue"]'
    Then I expect the HTML element ".q-item" appear 3 times on screen
    And I expect the HTML element '.q-item.q-item--active' not exists

    ## 105 Should allow selecting a role value
    When I click on '[data-cy="title"]'
    Then I expect the HTML element '.q-menu' not exists
    When I select ".q-item:nth-child(1)" in "[data-cy='field_role']"
    Then I expect the HTML element '[data-cy="field_role"] .q-select__focus-target' to have value "user"

    ####################################################
    ################## Create User with Roles ##########
    ####################################################

    ## 201 Should display required validation error message
    And I expect the HTML element '[data-cy="field-container_roleWithInvalidDefaultValue"] .q-field__messages' to be visible
    And I expect the HTML element '[data-cy="field-container_roleWithInvalidDefaultValue"] .q-field__messages' contains "Le rôle est requis."

    ## 202 Should successfully create user with selected roles
    When I set the text "staticlist.test@example.com" in the HTML element '[data-cy="field_email"]'
    And I set the text "Static" in the HTML element '[data-cy="field_firstName"]'
    And I set the text "ListTest" in the HTML element '[data-cy="field_lastName"]'
    And I select ".q-item:nth-child(3)" in "[data-cy='field_roleWithInvalidDefaultValue']"
    Then I expect the HTML element '.q-menu' not exists
    And I expect the HTML element '[data-cy="field_roleWithInvalidDefaultValue"] .q-select__focus-target' to have value "manager"
    When I click on '[data-cy="button_confirm"]'

    ## 203 Should display success notification after creation
    Then I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Utilisateur créé avec succès"

    ## 204 Should redirect to detail page after creation
    And I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="user-details-page_title"]' to be visible

    ## 205 Should display selected roles values on detail page
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "user"
    And I expect the HTML element '[data-cy="information-card--roleWithDefaultValue"] [data-cy="value"]' contains "admin"
    And I expect the HTML element '[data-cy="information-card--roleWithInvalidDefaultValue"] [data-cy="value"]' contains "manager"

    ####################################################
    ################## Edit User Role ##################
    ####################################################

    ## 301 Should display current roles on edit page
    When I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007/edit"
    And I expect the HTML element '[data-cy="title"]' contains "Édition de l'utilisateur"
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_role"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_roleWithDefaultValue"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_roleWithInvalidDefaultValue"]' to be visible

    ## 302 Should pre-fill roles fields with current value
    And I expect the HTML element '[data-cy="field_role"] .q-select__focus-target' to have value "user"
    And I expect the HTML element '[data-cy="field_roleWithDefaultValue"] .q-select__focus-target' to have value "admin"
    And I expect the HTML element '[data-cy="field_roleWithInvalidDefaultValue"] .q-select__focus-target' to have value "manager"

    ## 303 Should allow changing role to a different value
    When I select ".q-item:nth-child(3)" in "[data-cy='field_role']"
    Then I expect the HTML element '.q-menu' not exists
    And I expect the HTML element '[data-cy="field_role"] .q-select__focus-target' to have value "manager"
    When I click on '[data-cy="button_confirm"]'

    ## 304 Should display success notification after update
    Then I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Utilisateur mis à jour avec succès"

    ## 305 Should persist updated role on detail page
    And I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="user-details-page_title"]' to be visible
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "manager"
    And I expect the HTML element '[data-cy="information-card--roleWithDefaultValue"] [data-cy="value"]' contains "admin"
    And I expect the HTML element '[data-cy="information-card--roleWithInvalidDefaultValue"] [data-cy="value"]' contains "manager"

    ####################################################
    ################## Cleanup #########################
    ####################################################

    ## 401 Remove the test user
    When I request '{{ env.E2E_API_URL }}/api/users/00000000-0000-0000-0000-000000000007' with method 'DELETE'
    Then I expect status code is 204
