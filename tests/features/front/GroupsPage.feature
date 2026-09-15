Feature: Test Group homepage display

  ################## Group Homepage ##################
  ## 101 Should display the group homepage with all groups listed
  ## 102 Should filter groups when using advanced search

  Scenario: Roundtrip about Group homepage

    ####################################################
    ################## Authentication ##################
    ####################################################

    Given I set the viewport size to 1920 px by 1080 px
    And   I visit the '{{ env.E2E_FRONT_URL }}'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/"

    ####################################################
    ################## Group Homepage ##################
    ####################################################

    ## 101 Should display the group homepage with all groups listed
    # Backend Developers is the seeded group carrying every optional field.
    Given I store "groupId" as "00000000-0000-4000-8000-000000009006" in context
    When  I click on '[data-cy="item_moduleGroupsPage"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/groups"
    And   I expect the HTML element '.linid-smart-filter' to be visible
    And   I expect the HTML element '.generic-entity-table' to be visible
    # Six groups are seeded, see docker/init-db/scripts/init_linid.sql
    And   I expect the HTML element '[data-cy="item-row"]' appear 6 times on screen
    And   I expect the HTML element '[data-cy="cell-code_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-code_{{ctx.groupId}}"]' contains "backend-developers"
    And   I expect the HTML element '[data-cy="cell-name_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-name_{{ctx.groupId}}"]' contains "Backend Developers"
    And   I expect the HTML element '[data-cy="cell-parentName_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-parentName_{{ctx.groupId}}"]' contains "Developers"
    And   I expect the HTML element '[data-cy="cell-description_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-description_{{ctx.groupId}}"]' contains "Developers working on the API."
    And   I expect the HTML element '[data-cy="cell-organizationalUnitName_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-organizationalUnitName_{{ctx.groupId}}"]' contains "Dept A1-1"
    And   I expect the HTML element '[data-cy="cell-applicationName_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-applicationName_{{ctx.groupId}}"]' contains "LINID - Identity Manager"
    And   I expect the HTML element '[data-cy="cell-email_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-email_{{ctx.groupId}}"]' contains "backend-developers@example.com"
    And   I expect the HTML element '[data-cy="cell-createdBy_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-createdBy_{{ctx.groupId}}"]' contains "admin_fn admin_ln"
    And   I expect the HTML element '[data-cy="cell-insertDate_{{ctx.groupId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-table_actions_{{ctx.groupId}}"]' to be visible

    ## 102 Should filter groups when using advanced search
    When I click on ".linid-smart-filter"
    And  I set the text "unknown" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups?code=lk_*unknown*"
    And  I expect the HTML element '[data-cy="item-row"]' appear 0 times on screen

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 6 times on screen

    When I click on ".linid-smart-filter"
    And  I set the text "backend-developers" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups?code=lk_*backend-developers*"
    And  I expect the HTML element '[data-cy="item-row"]' to be visible
    And  I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.groupId}}"]' to be visible

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 6 times on screen
