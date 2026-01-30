Feature: Test Frontend Homepage

  ################## Homepage ##################
  ## 101 Should load the homepage successfully

  Scenario: 101 - Should load the homepage successfully
    Given I visit the '{{env.E2E_FRONT_URL}}'
    Then  I expect current url is '{{env.E2E_FRONT_URL}}/'
Feature: Test homepage

  ################## Application Display ##################
  ## 101 Should display application title, icon, and version
  ## 102 Should display correct navigation toolbars with menus

  ################## Content Sections ##################
  ## 201 Should display all home page sections

  ################## Navigation ##################
  ## 301 Should navigate to Users module
  ## 302 Should redirect to home from logo and title

    ####################################################
    ################## Application Display #############
    ####################################################

  ## 101 Should display application title, icon, and version
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}'
    Then I expect the HTML element '[data-cy="home-page"]' to be visible
    Then I expect the HTML element '[data-cy="application_title"]' contains 'Linid - Identity Manager'
    Then I expect the HTML element '[data-cy="application_logo"]' to be visible
    Then I expect the HTML element '[data-cy="application_version"]' contains 'Dev version' ######################################### Demander ce qu'il faut mettre

  ## 102 Should display correct navigation toolbars with menus
    Then I expect the HTML element '[data-cy="toolbar"]' exists
    Then I expect the HTML element '[data-cy="navigation_toolbar"]' exists
    Then I expect the HTML element '[data-cy="item_moduleUsers"]' exists

    ####################################################
    ################## Content Sections ################
    ####################################################

  ## 201 Should display all home page sections
    Then I expect the HTML element '[data-cy="home-page-intro"]' exists
    And I expect the HTML element "[data-cy="home-page-intro"]" not contains "" ########################## Est ce que c'est vraiment comme ca que l'on test si c'est pas vide
    Then I expect the HTML element '[data-cy="home-page-opensource"]' exists
    And I expect the HTML element "[data-cy="home-page-opensource"]" not contains ""
    Then I expect the HTML element '[data-cy="home-page-license"]' exists
    And I expect the HTML element "[data-cy="home-page-license"]" not contains ""
    Then I expect the HTML element '[data-cy="home-page-links"]' exists
    And I expect the HTML element "[data-cy="home-page-links"]" not contains ""
    Then I expect the HTML element '[data-cy="home-page-branding"]' exists
    And I expect the HTML element "[data-cy="home-page-branding"]" not contains ""

    ####################################################
    ################## Navigation #####################
    ####################################################

  ## 301 Should navigate to Users module
    When I click on '[data-cy="item_moduleUsers"]'
    Then I expect current url is "http://localhost:9000/users"

  ## 302 Should redirect to home from logo and title
    When I click on '[data-cy="application_logo"]'
    Then I expect current url is "http://localhost:9000/"
    When I click on '[data-cy="application_title"]'
    Then I expect current url is "http://localhost:9000/"
