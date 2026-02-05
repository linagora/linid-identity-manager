Feature: Test Module Users Edit and Create

  ################## User Edit ##################
  ## 101 Create a user to use for editing
  ## 102 Should redirect to user detail page when clicking View (John doe)
  ## 103 Should display all user information on detail page (John doe)
  ## 104 Should redirect to edit page and pre-fill fields correctly (John Doe)
  ## 105 Should successfully update user data and reflect changes in detail page
  ## 106 Should reflect updated user data in users table
  ## 107 Remove the user

  ################## User Creation ##################
  ## 201 Should navigate to create new user page
  ## 202 Should successfully create new user with valid data
  ## 203 Should successfully display all data in details vue
  ## 204 Should display newly created user in users table
  ## 205 Remove the user just created

  Scenario: Complete user editing and creation workflow
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/moduleUsers'

  ####################################################
  ################## User Edit ######################
  ####################################################

  ## 101 Create a user to use for editing
    And I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/api/users' with method 'POST' with body:
      """
      {"email": "test.edit@example.com", "firstName": "Test", "lastName": "Edit", "role": "Admin"}
      """
    Then I expect status code is 201

  ## 102 Should redirect to user detail page when clicking View (John doe)
    When I click on '[data-cy="see-button_{{response.body.id}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/{{response.body.id}}"
    #And I expect the HTML element '[data-cy="user-details-page--title"]' contains "Détails de l’utilisateur :"

  ## 103 Should display all user information on detail page (John doe)
    And I expect the HTML element '[data-cy="user-details-card"]' to be visible
    And I expect the HTML element '[data-cy="information-card--id"] [data-cy="value"]' contains "{{response.body.id}}"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "test.edit@example.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Test"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Edit"
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "Admin"

  ## 104 Should redirect to edit page and pre-fill fields correctly (John Doe)
    And I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    And I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/{{response.body.id}}/edit"
    And I expect the HTML element '[data-cy="field_email"]' to have value "test.edit@example.com"
    And I expect the HTML element '[data-cy="field_firstName"]' to have value "Test"
    And I expect the HTML element '[data-cy="field_lastName"]' to have value "Edit"
    And I expect the HTML element '[data-cy="field_role"]' to have value "Admin"

  ## 105 Should successfully update user data and reflect changes in detail page
    When I set the text "Jean.dupont@example.com" in the HTML element '[data-cy="field_email"]'
    And I set the text "Jean" in the HTML element '[data-cy="field_firstName"]'
    And I set the text "Dupont" in the HTML element '[data-cy="field_lastName"]'
    And I set the text "User" in the HTML element '[data-cy="field_role"]'
    And I click on '[data-cy="button_confirm"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/{{response.body.id}}"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "Jean.dupont@example.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Jean"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Dupont"
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "User"

  ## 106 Should reflect updated user data in users table
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers"
    And I expect the HTML element '[data-cy="cell-email_{{response.body.id}}"]' contains "Jean.dupont@example.com"
    And I expect the HTML element '[data-cy="cell-firstName_{{response.body.id}}"]' contains "Jean"
    And I expect the HTML element '[data-cy="cell-lastName_{{response.body.id}}"]' contains "Dupont"

  ## 107 Remove the user
    When I request '{{env.E2E_API_URL}}/api/users/{{response.body.id}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## User Creation ##################
  ####################################################

  ## 201 Should navigate to create new user page
    When I click on '[data-cy="button_create"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/new"
    And I expect the HTML element '[data-cy="new-user-page"]' to be visible

  ## 202 Should successfully create new user with valid data
    When I set the text "michael.scott@gmail.com" in the HTML element '[data-cy="field_email"]'
    Then I set the text "Michael" in the HTML element '[data-cy="field_firstName"]'
    And I set the text "Scott" in the HTML element '[data-cy="field_lastName"]'
    And I set the text "Admin" in the HTML element '[data-cy="field_role"]'
    And I click on '[data-cy="button_confirm"]'

  ## 203 Should successfully display all data in details vue
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "michael.scott@gmail.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Michael"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Scott"
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "Admin"

  ## 204 Should display newly created user in users table
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers"
    And I expect the HTML element '[data-cy="user-row"]' appear 7 times on screen
    And I expect the HTML element '[data-cy="cell-email_00000000-0000-0000-0000-000000000007"]' contains "michael.scott@gmail.com"
    And I expect the HTML element '[data-cy="cell-firstName_00000000-0000-0000-0000-000000000007"]' contains "Michael"
    And I expect the HTML element '[data-cy="cell-lastName_00000000-0000-0000-0000-000000000007"]' contains "Scott"

  ## 205 Remove the user just created
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000007' with method 'DELETE'
    Then I expect status code is 204
