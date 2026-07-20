Feature: Test Organizational Units page display

  ################## Organizational Units Page ##################
  ## 101 Should display the Organizational Units tab in the main menu
  ## 102 Should display the page with the smart filter and the OU table
  ## 103 Should show the details of a OU when click see button of a OU
  ## 104 Should show the details of another OU when click see button of a OU

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
    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"

    ## 102 Should display the page with the smart filter and the OU table
    And I expect the HTML element '[data-cy="item-row"]' to be visible
    And I expect the HTML element '[data-cy="linid-smart-filter-field"]' to be visible
    And I expect the HTML element '[data-cy="generic-entity-table"]' to be visible

    ## 103 Should show the details of a OU when click see button of a OU
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "Team Beta" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="cell-name_00000000-0000-4000-8000-000000000ee1"]' to be visible
    And I expect the HTML element '[data-cy="cell-name_00000000-0000-4000-8000-000000000ee1"]' contains "Team Beta"
    And I expect the HTML element '[data-cy="cell-type_00000000-0000-4000-8000-000000000ee1"]' to be visible
    And I expect the HTML element '[data-cy="cell-type_00000000-0000-4000-8000-000000000ee1"]' contains "TEAM"
    And I expect the HTML element '[data-cy="cell-createdBy_00000000-0000-4000-8000-000000000ee1"]' to be visible
    And I expect the HTML element '[data-cy="cell-createdBy_00000000-0000-4000-8000-000000000ee1"]' contains "admin_fn admin_ln"
    When I click on '[data-cy="see-button_00000000-0000-4000-8000-000000000ee1"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/00000000-0000-4000-8000-000000000ee1"
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Team Beta"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "TEAM"

    ## 104 Should show the details of another OU when click see button of a OU
    When I click on '[data-cy="button_cancel"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "SuspendedOuWithEnd" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="cell-name_00000000-0000-4000-8000-0000000000e3"]' to be visible
    And I expect the HTML element '[data-cy="cell-name_00000000-0000-4000-8000-0000000000e3"]' contains "SuspendedOuWithEnd"
    And I expect the HTML element '[data-cy="cell-type_00000000-0000-4000-8000-0000000000e3"]' to be visible
    And I expect the HTML element '[data-cy="cell-type_00000000-0000-4000-8000-0000000000e3"]' contains "COMPANY"
    And I expect the HTML element '[data-cy="cell-createdBy_00000000-0000-4000-8000-0000000000e3"]' to be visible
    And I expect the HTML element '[data-cy="cell-createdBy_00000000-0000-4000-8000-0000000000e3"]' contains "admin_fn admin_ln"
    When I click on '[data-cy="see-button_00000000-0000-4000-8000-0000000000e3"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/00000000-0000-4000-8000-0000000000e3"
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "SuspendedOuWithEnd"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "COMPANY"
