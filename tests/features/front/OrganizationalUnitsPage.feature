Feature: Test Organizational Units page display

  ################## Organizational Units Page ##################
  ## 101 Should display the Organizational Units tab in the main menu
  ## 102 Should display the page with the splitter layout and the OU tree
  ## 103 Should select the root OU by default and display its details
  ## 104 Should update the details panel when selecting a company OU node
  ## 105 Should update the details panel when selecting another company OU node
  ## 106 Should select a nested OU node deeper in the tree
  ## 107 Should select the OU referenced by the node query parameter on load
  ## 108 Should keep the selected OU node when navigating between accountsPage and organizationalUnitsPage

  Scenario: Roundtrip about Organizational Units page

    ####################################################
    ################## Authentication ##################
    ####################################################

    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}'
    When I set the text "admin" in the HTML element "input#userfield"
    And I set the text "password" in the HTML element "input#passwordfield"
    And I click on "button.btn-success"
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/"

    ####################################################
    ################## Organizational Units Page #######
    ####################################################

    ## 101 Should display the Organizational Units tab in the main menu
    When I click on '[data-cy="item_organizational-units"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"

    ## 102 Should display the page with the splitter layout and the OU tree
    And I expect the HTML element '[data-cy="organizational-unit-details-page"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-details-page_title"]' to be visible
    And I expect the HTML element '[data-cy="generic-tree"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000aa"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000bb"]' appear 1 times on screen

    ## 103 Should select the root OU by default and display its details
    And I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units?node="
    And I expect the HTML element '[data-cy="organizational-unit-details-page_cards"]' to be visible
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "root"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "root"

    ## 104 Should update the details panel when selecting a company OU node
    When I click on '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000aa"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units?node=00000000-0000-4000-8000-0000000000aa"
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Company A"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "COMPANY"

    ## 105 Should update the details panel when selecting another company OU node
    When I click on '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000bb"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units?node=00000000-0000-4000-8000-0000000000bb"
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Company B"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "COMPANY"

    ## 106 Should select a nested OU node deeper in the tree
    When I click on '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000cc"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units?node=00000000-0000-4000-8000-0000000000cc"
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Division A1"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "DIVISION"

    ## 107 Should select the OU referenced by the node query parameter on load
    When I click on '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000bb"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units?node=00000000-0000-4000-8000-0000000000bb"
    And I expect the HTML element '[data-cy="organizational-unit-details-page_cards"]' to be visible
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Company B"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "COMPANY"

    ## 108 Should keep the selected OU node when navigating between accountsPage and organizationalUnitsPage
    When I click on '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000cc"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units?node=00000000-0000-4000-8000-0000000000cc"
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Division A1"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "DIVISION"
    When I click on '[data-cy="item_accounts"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts?node=00000000-0000-4000-8000-0000000000cc"
    And I expect the HTML element '[data-cy="cell-firstname"]' contains "user3_fn"
    And I expect the HTML element '[data-cy="cell-lastname"]' contains "user3_ln"
    And I expect the HTML element '[data-cy="cell-email"]' contains "user3@example.com"
    And I expect the HTML element '[data-cy="cell-createdBy"]' contains "admin_fn admin_ln"
    When I click on '[data-cy="item_organizational-units"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units?node=00000000-0000-4000-8000-0000000000cc"
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Division A1"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "DIVISION"
