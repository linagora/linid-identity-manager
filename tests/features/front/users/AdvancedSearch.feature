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
  ## 304 Should collapse advanced section when clicking toggle button again

  ################## Advanced Fields #################
  ## 401 Should display lastName field in advanced section
  ## 402 Should have no validation constraints on advanced fields

  ####################################################
  ################## Navigation ######################
  ####################################################

  Scenario: Should navigate to users module and display advanced search
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    Then  I expect the HTML element '[data-cy="advanced-search-card"]' exists
    And   I expect the HTML element '[data-cy="advanced-search-card"]' to be visible

  ####################################################
  ################## Default Fields ##################
  ####################################################

  Scenario: Should display default search fields (email, firstName)
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    Then  I expect the HTML element '[data-cy="field_email"]' exists
    And   I expect the HTML element '[data-cy="field_email"]' to be visible
    And   I expect the HTML element '[data-cy="field_firstName"]' exists
    And   I expect the HTML element '[data-cy="field_firstName"]' to be visible

  Scenario: Should have no validation constraints on default fields
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    Then  I expect the HTML element '[data-cy="field_email"]' to be enabled
    And   I expect the HTML element '[data-cy="field_firstName"]' to be enabled

  ####################################################
  ################## Expand/Collapse #################
  ####################################################

  Scenario: Should display toggle button for advanced fields
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    Then  I expect the HTML element '[data-cy="advanced-search-card--toggle-button"]' exists
    And   I expect the HTML element '[data-cy="advanced-search-card--toggle-button"]' to be visible

  Scenario: Should hide advanced section by default
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    Then  I expect the HTML element '[data-cy="advanced-search-card--advanced-section"]' to be hidden

  Scenario: Should expand advanced section when clicking toggle button
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    And   I click on '[data-cy="advanced-search-card--toggle-button"]'
    Then  I expect the HTML element '[data-cy="advanced-search-card--advanced-section"]' to be visible

  Scenario: Should collapse advanced section when clicking toggle button again
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    And   I click on '[data-cy="advanced-search-card--toggle-button"]'
    And   I click on '[data-cy="advanced-search-card--toggle-button"]'
    Then  I expect the HTML element '[data-cy="advanced-search-card--advanced-section"]' to be hidden

  ####################################################
  ################## Advanced Fields #################
  ####################################################

  Scenario: Should display lastName field in advanced section
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    And   I click on '[data-cy="advanced-search-card--toggle-button"]'
    Then  I expect the HTML element '[data-cy="field_lastName"]' exists
    And   I expect the HTML element '[data-cy="field_lastName"]' to be visible

  Scenario: Should have no validation constraints on advanced fields
    Given I visit the '{{env.E2E_FRONT_URL}}/moduleUsers'
    And   I click on '[data-cy="advanced-search-card--toggle-button"]'
    Then  I expect the HTML element '[data-cy="field_lastName"]' to be enabled
