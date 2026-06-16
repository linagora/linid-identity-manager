Feature: Test homepage

  ################## Application Display ##################
  ## 101 Should display application title, icon, and version

  ################## Header Profile Menu ##################
  ## 201 Should display user profile menu

  ################## Content Sections ##################
  ## 301 Should display all home page sections

  Scenario: Roundtrip in the application
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"

  ####################################################
  ################## Application Display #############
  ####################################################

  ## 101 Should display application title, icon, and version
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    And I expect the HTML element '[data-cy="application_title"]' contains 'LinID Identity Manager'
    And I expect the HTML element '[data-cy="application_logo"]' to be visible
    And I expect the HTML element '[data-cy="application_version"]' contains "Version de développement"
    And I expect the HTML element '[data-cy="header_profile_button"]' to be visible

  ####################################################
  ################## Header Profile Menu #############
  ####################################################

  ## 201 Should display user profile menu
    When I click on '[data-cy="header_profile_button"]'
    Then I expect the HTML element '[data-cy="header_profile_menu"]' to be visible
    And I expect the HTML element '[data-cy="header_profile_name"]' contains "admin name"
    And I expect the HTML element '[data-cy="header_profile_email"]' contains "admin@example.com"

  ####################################################
  ################## Content Sections ################
  ####################################################

  ## 301 Should display all home page sections
    And I expect the HTML element '[data-cy="home-page-intro"]' exists
    And I expect the HTML element '[data-cy="home-page-intro"]' contains "LinID Identity Manager"
    And I expect the HTML element '[data-cy="home-page-opensource"]' exists
    And I expect the HTML element '[data-cy="home-page-opensource"]' contains "Projet open source"
    And I expect the HTML element '[data-cy="home-page-license"]' exists
    And I expect the HTML element '[data-cy="home-page-license"]' contains "GNU Affero General Public License"
    And I expect the HTML element '[data-cy="home-page-links"]' exists
    And I expect the HTML element '[data-cy="home-page-links"]' contains "Code source disponible sur"
    And I expect the HTML element '[data-cy="home-page-branding"]' exists
    And I expect the HTML element '[data-cy="home-page-branding"]' contains "Un projet de"
