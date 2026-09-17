Feature: Test Role homepage display

  ################## Role Homepage ##################
  ## 101 Should display the role homepage with all roles listed
  ## 102 Should filter roles when using advanced search

  Scenario: Roundtrip about Role homepage

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
    ################## Role Homepage ##################
    ####################################################

    ## 101 Should display the role homepage with all roles listed
    # Auditor is the seeded role carrying every optional field.
    Given I store "roleId" as "00000000-0000-4000-8000-00000000b006" in context
    When  I click on '[data-cy="item_moduleRolesPage"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/roles"
    And   I expect the HTML element '.linid-smart-filter' to be visible
    And   I expect the HTML element '.generic-entity-table' to be visible
    # Six roles are seeded, see docker/init-db/scripts/init_linid.sql
    And   I expect the HTML element '[data-cy="item-row"]' appear 6 times on screen
    And   I expect the HTML element '[data-cy="cell-code_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-code_{{ctx.roleId}}"]' contains "auditor"
    And   I expect the HTML element '[data-cy="cell-name_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-name_{{ctx.roleId}}"]' contains "Auditor"
    And   I expect the HTML element '[data-cy="cell-description_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-description_{{ctx.roleId}}"]' contains "Reviews compliance and security controls."
    And   I expect the HTML element '[data-cy="cell-createdBy_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-createdBy_{{ctx.roleId}}"]' contains "admin_fn admin_ln"
    And   I expect the HTML element '[data-cy="cell-insertDate_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-table_actions_{{ctx.roleId}}"]' to be visible

    ## 102 Should filter roles when using advanced search
    When I click on ".linid-smart-filter"
    And  I set the text "unknown" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles?code=lk_*unknown*"
    And  I expect the HTML element '[data-cy="item-row"]' appear 0 times on screen

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 6 times on screen

    When I click on ".linid-smart-filter"
    And  I set the text "auditor" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles?code=lk_*auditor*"
    And  I expect the HTML element '[data-cy="item-row"]' to be visible
    And  I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.roleId}}"]' to be visible

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 6 times on screen
