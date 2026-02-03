Feature: Test Frontend Advanced Search

  ################## Navigation ######################
  ## 101 Should navigate to users module and display advanced search

  ################## Default Fields ##################
  ## 201 Should display default search fields (email, firstName)
  ## 202 Should have no validation constraints on default fields

  ################## Expand/Collapse #################
  ## 301 Should display toggle button for advanced fields
  ## 302 Should hide advanced section by default
  ## 303 Should expand advanced section when clicking toggle button

  ################## Advanced Fields #################
  ## 401 Should display lastName field in advanced section
  ## 402 Should have no validation constraints on advanced fields
  ## 403 Should collapse advanced section when clicking toggle button again

  ####################### Search #####################
  ## 501 Should verify initial state: 5 users in table
  ## 502 Should filter table data when searching by email
  ## 503 Should filter table data when searching by firstName
  ## 504 Should filter table data when searching by lastName (advanced field)

  Scenario: Roundtrip on advanced search
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'

    ####################################################
    ################## Navigation ######################
    ####################################################
    # 101 Should navigate to users module and display advanced search
    Then I expect the HTML element '[data-cy="advanced-search-card"]' exists
    And  I expect the HTML element '[data-cy="advanced-search-card"]' to be visible

    ####################################################
    ################## Default Fields ##################
    ####################################################
    # 201 Should display default search fields (email, firstName)
    And  I expect the HTML element '[data-cy="field_email"]' exists
    And  I expect the HTML element '[data-cy="field_email"]' to be visible
    And  I expect the HTML element '[data-cy="field_firstName"]' exists
    And  I expect the HTML element '[data-cy="field_firstName"]' to be visible

    # 202 Should have no validation constraints on default fields
    And  I expect the HTML element '[data-cy="field_email"]' to be enabled
    And  I expect the HTML element '[data-cy="field_firstName"]' to be enabled

    ####################################################
    ################## Expand/Collapse #################
    ####################################################
    # 301 Should display toggle button for advanced fields
    And  I expect the HTML element '[data-cy="advanced-search-card--toggle-button"]' exists
    And  I expect the HTML element '[data-cy="advanced-search-card--toggle-button"]' to be visible

    # 302 Should hide advanced section by default
    And  I expect the HTML element '[data-cy="advanced-search-card--advanced-section"]' to be hidden

    # 303 Should expand advanced section when clicking toggle button
    When I click on '[data-cy="advanced-search-card--toggle-button"]'
    Then I expect the HTML element '[data-cy="advanced-search-card--advanced-section"]' to be visible

    ####################################################
    ################## Advanced Fields #################
    ####################################################
    # 401 Should display lastName field in advanced section
    And  I expect the HTML element '[data-cy="field_lastName"]' exists
    And  I expect the HTML element '[data-cy="field_lastName"]' to be visible

    # 402 Should have no validation constraints on advanced fields
    And  I expect the HTML element '[data-cy="field_lastName"]' to be enabled

    # 403 Should collapse advanced section when clicking toggle button again
    When I click on '[data-cy="advanced-search-card--toggle-button"]'
    Then I expect the HTML element '[data-cy="advanced-search-card--advanced-section"]' to be hidden

    ####################################################
    ####################### Search #####################
    ####################################################
    # 501 Should verify initial state: 5 users in table
    And  I expect the HTML element '.q-table tbody tr' appear 5 times on screen

    # 502 Should filter table data when searching by email
    When I set the text 'alice.smith' in the HTML element 'input[data-cy="field_email"]'
    And  I click on '[data-cy="advanced-search-card"]'
    And  I wait 1s
    Then I expect the HTML element '.q-table tbody tr' appear 1 time on screen
    And  I expect the HTML element '.q-table tbody' contains 'alice.smith@example.com'
    And  I expect the HTML element '.q-table tbody' not contains 'jane.roe@example.com'

    # 503 Should filter table data when searching by firstName
    When I clear the text in the HTML element 'input[data-cy="field_email"]'
    And  I set the text 'Alice' in the HTML element 'input[data-cy="field_firstName"]'
    And  I click on '[data-cy="advanced-search-card"]'
    And  I wait 1s
    Then I expect the HTML element '.q-table tbody tr' appear 1 time on screen
    And  I expect the HTML element '.q-table tbody' contains 'Alice'
    And  I expect the HTML element '.q-table tbody' not contains 'Jane'

    # 504 Should filter table data when searching by lastName (advanced field)
    When I clear the text in the HTML element 'input[data-cy="field_firstName"]'
    And  I click on '[data-cy="advanced-search-card--toggle-button"]'
    And  I set the text 'Smith' in the HTML element 'input[data-cy="field_lastName"]'
    And  I click on '[data-cy="advanced-search-card"]'
    And  I wait 1s
    Then I expect the HTML element '.q-table tbody tr' appear 1 time on screen
    And  I expect the HTML element '.q-table tbody' contains 'Smith'
    And  I expect the HTML element '.q-table tbody' not contains 'Johnson'
