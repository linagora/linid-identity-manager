Feature: Test Module Users

  ################## Module Display ##################
  ## 101 Should display correct title on Users module landing page
  ## 102 Should display users search panel and search field
  ## 103 Should display users table with expected data

  ################## User Details ##################
  ## 201 Should redirect to user detail page when clicking View (John doe)
  ## 202 Should display all user information on detail page (John doe)
  ## 203 Should display all action buttons on detail page (John doe)
  ## 204 Edit button should go to the edit page (Jane Roe)
  ## 205 Cancel button should come back at moduleUser homepage (John doe)
  ## 206 Should redirect to user detail page when clicking View (Jane Roe)
  ## 207 Should display all user information on detail page (Jane Roe)
  ## 208 Should display all action buttons on detail page (Jane Roe)
  ## 209 Edit button should go to the edit page (Jane Roe)
  ## 210 Cancel button should come back at moduleUser homepage (Jane Roe)

  ################## Table Features ##################
  ## 301 Should correctly handle pagination
  ## 302 Should correctly handle simple search in users table

  Scenario: Roundtrip about Module Users
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/users'

    ####################################################
    ################## Module Display #################
    ####################################################

  ## 101 Should display correct title on Users module landing page
    Then I expect the HTML element '[data-cy="module-user-title"]' to be visible
    And I expect the HTML element '[data-cy="module-user-title"]' contains 'Utilisateurs'

  ## 102 Should display users search panel and search field
    And I expect the HTML element '[data-cy="advanced-search-card"]' to be visible
    And I expect the HTML element '[data-cy="field_email"]' to be visible
    And I expect the HTML element '[data-cy="field_firstName"]' to be visible
    And I expect the HTML element '[data-cy="field_lastName"]' to be hidden
    When I click on '[data-cy="advanced-search-card--toggle-button"]'
    Then I expect the HTML element '[data-cy="field_lastName"]' to be visible

  ## 103 Should display users table with expected data
    And I expect the HTML element '[data-cy="entity-table"]' exists
    And I expect the HTML element '[data-cy="user-row"]' appear 6 times on screen
    And I expect the HTML element '[data-cy="item-user-cell-email_00000000-0000-0000-0000-000000000000"]' contains "john.doe@gmail.com"
    And I expect the HTML element '[data-cy="item-user-cell-firstName_00000000-0000-0000-0000-000000000000"]' contains "John"
    And I expect the HTML element '[data-cy="item-user-cell-lastName_00000000-0000-0000-0000-000000000000"]' contains "Doe"
    And I expect the HTML element '[data-cy="item-user-cell-email_00000000-0000-0000-0000-000000000001"]' contains "jane.roe@gmail.com"
    And I expect the HTML element '[data-cy="item-user-cell-firstName_00000000-0000-0000-0000-000000000001"]' contains "Jane"
    And I expect the HTML element '[data-cy="item-user-cell-lastName_00000000-0000-0000-0000-000000000001"]' contains "Roe"

    ####################################################
    ################## User Details ###################
    ####################################################

  ## 201 Should redirect to user detail page when clicking View (John doe)
    When I click on '[data-cy="see-user-button_00000000-0000-0000-0000-000000000000"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000000"
    And I expect the HTML element '[data-cy="userDetailsPage"]' to be visible

  ## 202 Should display all user information on detail page (John doe)
    And I expect the HTML element '[data-cy="user-details-card"]' to be visible
    And I expect the HTML element '[data-cy="information-card--id"] [data-cy="value"]' contains "00000000-0000-0000-0000-000000000000"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "john.doe@gmail.com"
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "John"
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Doe"
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "admin"

  ## 203 Should display all action buttons on detail page (John doe)
    And I expect the HTML element '[data-cy="buttons-card"]' to be visible
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_cancel"]' contains "Retour à la liste des utilisateurs"
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_edit"]' contains "Modifier"

  ## 204 Edit button should go to the edit page (John doe)
    When I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000000/edit"
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000000"

  ## 205 Cancel button should come back at moduleUser homepage (John doe)
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "https://localhost:9000/users"

  ## 206 Should redirect to user detail page when clicking View (Jane Roe)
    When I click on '[data-cy="see-user-button_00000000-0000-0000-0000-000000000001"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000001"
    And I expect the HTML element '[data-cy="userDetailsPage"]' to be visible

  ## 207 Should display all user information on detail page (Jane Roe)
    And I expect the HTML element '[data-cy="user-details-card"]' to be visible
    And I expect the HTML element '[data-cy="information-card--id"]' to be visible
    And I expect the HTML element '[data-cy="information-card--id"] [data-cy="value"]' contains "00000000-0000-0000-0000-000000000001"
    And I expect the HTML element '[data-cy="information-card--email"]' to be visible
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "jane.roe@gmail.com"
    And I expect the HTML element '[data-cy="information-card--firstName"]' to be visible
    And I expect the HTML element '[data-cy="information-card--firstName"] [data-cy="value"]' contains "Jane"
    And I expect the HTML element '[data-cy="information-card--lastName"]' to be visible
    And I expect the HTML element '[data-cy="information-card--lastName"] [data-cy="value"]' contains "Roe"
    And I expect the HTML element '[data-cy="information-card--role"]' to be visible
    And I expect the HTML element '[data-cy="information-card--role"] [data-cy="value"]' contains "user"

  ## 208 Should display all action buttons on detail page (Jane Roe)
    And I expect the HTML element '[data-cy="buttons-card"]' to be visible
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_cancel"]' to be visible
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_cancel"]' contains "Retour à la liste des utilisateurs"
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_edit"]' to be visible
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_edit"]' contains "Modifier"

  ## 204 Edit button should go to the edit page (Jane Roe)
    When I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000001/edit"
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "https://localhost:9000/users/00000000-0000-0000-0000-000000000001"

  ## 205 Cancel button should come back at moduleUser homepage (Jane Roe)
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "https://localhost:9000/users"

    ####################################################
    ################## Table Features ##################
    ####################################################

  ## 301 Should correctly handle pagination
    When I click on '.q-select__dropdown-icon'
    Then I click on '.q-virtual-scroll__content > :first-child'
    And I expect the HTML element '[data-cy="user-row"]' appear 5 times on screen
    When I click on '.q-btn--actionable[aria-label="Next page"]'
    Then I expect the HTML element '[data-cy="user-row"]' appear 1 times on screen

  ## 302 Should correctly handle simple search in users table
    When I set the text "John" in the HTML element '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="user-row"]' appear 2 times on screen
    And I expect the HTML element '[data-cy="item-user-cell-email_00000000-0000-0000-0000-000000000000"]' contains "john.doe@gmail.com"
    And I expect the HTML element '[data-cy="item-user-cell-firstName_00000000-0000-0000-0000-000000000000"]' contains "John"
    And I expect the HTML element '[data-cy="item-user-cell-lastName_00000000-0000-0000-0000-000000000000"]' contains "Doe"
    When I clear the text in the HTML element '[data-cy="field_email"]'
    And I set the text "Jane" in the HTML element '[data-cy="field_firstName"]'
    Then I expect the HTML element '[data-cy="item-user-cell-email_00000000-0000-0000-0000-000000000001"]' contains "jane.roe@gmail.com"
    And I expect the HTML element '[data-cy="item-user-cell-firstName_00000000-0000-0000-0000-000000000001"]' contains "Jane"
    And I expect the HTML element '[data-cy="item-user-cell-lastName_00000000-0000-0000-0000-000000000001"]' contains "Roe"
    When I clear the text in the HTML element '[data-cy="field_firstName"]'
    And I click on '[data-cy="advanced-search-card--toggle-button"]'
    And I set the text "Martin" in the HTML element '[data-cy="field_lastName"]'
    Then I expect the HTML element '[data-cy="item-user-cell-email_00000000-0000-0000-0000-000000000002"]' contains "alice.martin@example.com"
    And I expect the HTML element '[data-cy="item-user-cell-firstName_00000000-0000-0000-0000-000000000002"]' contains "Alice"
    And I expect the HTML element '[data-cy="item-user-cell-lastName_00000000-0000-0000-0000-000000000002"]' contains "Martin"
