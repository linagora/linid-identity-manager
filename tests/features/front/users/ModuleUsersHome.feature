Feature: Test homepage

  ################## Application Display ##################
  ## 101 Should display application title, icon, and version
  ## 102 Should display correct navigation toolbars with menus

  ################## Content Sections ##################
  ## 201 Should display all home page sections

  ################## Navigation ##################
  ## 301 Should navigate to Users module
  ## 302 Should redirect to home from logo and title

  Scenario: Roundtrip in the application
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/'

    ####################################################
    ################## Application Display #############
    ####################################################

  ## 101 Should display application title, icon, and version
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    And I expect the HTML element '[data-cy="application_title"]' contains 'Linid - Identity Manager'
    And I expect the HTML element '[data-cy="application_logo"]' to be visible
    And I expect the HTML element '[data-cy="application_version"]' contains 'Dev version'

  ## 102 Should display correct navigation toolbars with menus
    And I expect the HTML element '[data-cy="toolbar"]' exists
    And I expect the HTML element '[data-cy="navigation_toolbar"]' exists
    And I expect the HTML element '[data-cy="item_moduleUsers"]' exists

    ####################################################
    ################## Content Sections ################
    ####################################################

  ## 201 Should display all home page sections
    And I expect the HTML element '[data-cy="home-page-intro"]' exists
    And I expect the HTML element '[data-cy="home-page-intro"]' contains "LinID Identity Manager"
    And I expect the HTML element '[data-cy="home-page-opensource"]' exists
    And I expect the HTML element '[data-cy="home-page-opensource"]' contains "Projet open source"
    And I expect the HTML element '[data-cy="home-page-license"]' exists
    And I expect the HTML element '[data-cy="home-page-license"]' contains "GNU Affero General Public License v3"
    And I expect the HTML element '[data-cy="home-page-links"]' exists
    And I expect the HTML element '[data-cy="home-page-links"]' contains "Code source disponible"
    And I expect the HTML element '[data-cy="home-page-branding"]' exists
    And I expect the HTML element '[data-cy="home-page-branding"]' contains "LINAGORA"

    ####################################################
    ################## Navigation #####################
    ####################################################

  ## 301 Should navigate to Users module
    When I click on '[data-cy="item_moduleUsers"]'
    Then I expect current url is "https://localhost:9000/users"

  ## 302 Should redirect to home from logo and title
    When I click on '[data-cy="application_logo"]'
    Then I expect current url is "https://localhost:9000/"
    When I click on '[data-cy="application_title"]'
    Then I expect current url is "https://localhost:9000/"
