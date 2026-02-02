Feature: Test Module Users Edit and Create

  ################## User Edit ##################
  ## 401 Should redirect to edit page and pre-fill fields correctly (John Doe)
  ## 402 Should display validation errors for invalid email format
  ## 403 Should display validation errors for empty required fields
  ## 404 Should successfully update user data and reflect changes in detail page
  ## 405 Should reflect updated user data in users table

  ################## User Creation ##################
  ## 501 Should navigate to create new user page
  ## 502 Should display validation errors for invalid data on create form
  ## 503 Should successfully create new user with valid data
  ## 504 Should display newly created user in users table
  ## 505 Should show correct user data on newly created user detail page

  Scenario: Complete user editing and creation workflow
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/users'

    ####################################################
    ################## User Edit ######################
    ####################################################

  ## 401 Should redirect to edit page and pre-fill fields correctly (John Doe)
    When I click on '[data-cy="see-button_00000000-0000-0000-0000-000000000000"]'
    And I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000000/edit"
    And I expect the HTML element '[data-cy="edit-user-page"]' to be visible
    And I expect the HTML element '[data-cy="field-email"]' to have value "john.doe@gmail.com"
    And I expect the HTML element '[data-cy="field-firstName"]' to have value "John"
    And I expect the HTML element '[data-cy="field-lastName"]' to have value "Doe"
    And I expect the HTML element '[data-cy="field-displayName"]' to have value "John Doe"
    And I expect the HTML element '[data-cy="field-role"]' contains "admin"

  ## 402 Should display validation errors for invalid email format
  ##### Add the tests

  ## 403 Should display validation errors for empty required fields
  ##### Add the tests

  ## 404 Should successfully update user data and reflect changes in detail page
    When I set the text "john.doe.updated@gmail.com" in the HTML element '[data-cy="field-email"]'
    And I set the text "Johnny" in the HTML element '[data-cy="field-firstName"]'
    And I set the text "Johnny Doe Updated" in the HTML element '[data-cy="field-displayName"]'
    And I click on '[data-cy="button_confirm"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000000"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "john.doe.updated@gmail.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Johnny"
    And I expect the HTML element '[data-cy="information-card--displayName"] [data-cy="value"]' contains "Johnny Doe Updated"

  ## 405 Should reflect updated user data in users table
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "https://localhost:9000/users"
    And I expect the HTML element '[data-cy="item-user-cell-email_00000000-0000-0000-0000-000000000000"]' contains "john.doe.updated@gmail.com"
    And I expect the HTML element '[data-cy="item-user-cell-firstName_00000000-0000-0000-0000-000000000000"]' contains "Johnny"
    And I expect the HTML element '[data-cy="item-user-cell-displayName_00000000-0000-0000-0000-000000000000"]' contains "Johnny"

    ####################################################
    ################## User Creation ##################
    ####################################################

  ## 501 Should navigate to create new user page
    And I click on '[data-cy="button_create"]'
    Then I expect current url is "https://localhost:9000/users/create"
    And I expect the HTML element '[data-cy="new-user-page"]' to be visible
    And I expect the HTML element '[data-cy="field-email"]' to have value ""
    And I expect the HTML element '[data-cy="field-firstName"]' to have value ""
    And I expect the HTML element '[data-cy="field-lastName"]' to have value ""

  ## 502 Should display validation errors for invalid data on create form
  ##### Add the tests

  ## 503 Should successfully create new user with valid data
    When I set the text "michael.scott@gmail.com" in the HTML element '[data-cy="field-email"]'
    And I set the text "Michael" in the HTML element '[data-cy="field-firstName"]'
    And I set the text "Scott" in the HTML element '[data-cy="field-lastName"]'
    And I set the text "Michael Scott" in the HTML element '[data-cy="field-displayName"]'
    And I click on '[data-cy="button_confirm"]'
    And I expect the HTML element '[data-cy="userDetailsPage"]' to be visible

  ## 504 Should display newly created user in users table
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "https://localhost:9000/users"
    And I expect the HTML element '[data-cy="user-row"]' appear 7 times on screen
    And I expect the HTML element containing "michael.scott@gmail.com" to be visible
    And I expect the HTML element containing "Michael" to be visible
    And I expect the HTML element containing "Scott" to be visible

  ## 505 Should show correct user data on newly created user detail page
    When I click on the HTML element containing "michael.scott@gmail.com" within '[data-cy="user-row"]'
    Then I expect the HTML element '[data-cy="new-user-page"]' to be visible
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "michael.scott@gmail.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Michael"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Scott"
    And I expect the HTML element '[data-cy="information-card--displayName"] [data-cy="value"]' contains "Michael Scott"
