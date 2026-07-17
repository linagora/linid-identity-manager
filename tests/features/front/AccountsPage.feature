Feature: Test Account homepage display

  ################## Account Homepage ##################
  ## 101 Should display the account homepage with all accounts listed
  ## 102 Should filter accounts when using advanced search
  ## 103 Should go to detail page when click on account detail button
  ## 104 Should have pagination working

  Scenario: Roundtrip about Account homepage

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
    ################## Account Homepage ################
    ####################################################

    ## 101 Should display the account homepage with all accounts listed
    When I click on '[data-cy="item_moduleAccountsPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
    And I expect the HTML element '.linid-smart-filter' to be visible
    And I expect the HTML element '.generic-entity-table' to be visible
    And I expect the HTML element '[data-cy="item-row"]' appear 10 times on screen
    And I expect the HTML element '[data-cy="cell-firstname_00000000-0000-4000-8000-00000000a001"]' to be visible
    And I expect the HTML element '[data-cy="cell-firstname_00000000-0000-4000-8000-00000000a001"]' contains "admin_fn"
    And I expect the HTML element '[data-cy="cell-lastname_00000000-0000-4000-8000-00000000a001"]' to be visible
    And I expect the HTML element '[data-cy="cell-lastname_00000000-0000-4000-8000-00000000a001"]' contains "admin_ln"
    And I expect the HTML element '[data-cy="cell-email_00000000-0000-4000-8000-00000000a001"]' to be visible
    And I expect the HTML element '[data-cy="cell-email_00000000-0000-4000-8000-00000000a001"]' contains "admin@example.com"
    And I expect the HTML element '[data-cy="cell-createdBy_00000000-0000-4000-8000-00000000a001"]' to be visible
    And I expect the HTML element '[data-cy="cell-createdBy_00000000-0000-4000-8000-00000000a001"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="cell-insertDate_00000000-0000-4000-8000-00000000a001"]' to be visible
    And I expect the HTML element '[data-cy="cell-insertDate_00000000-0000-4000-8000-00000000a001"]' contains "{{ctx.accountDate}}"
    And I expect the HTML element '[data-cy="cell-table_actions_00000000-0000-4000-8000-00000000a001"]' to be visible

    ## 102 Should filter accounts when using advanced search
    Given I store "user_id" as "00000000-0000-4000-8000-00000000a006" in context
    When  I click on ".linid-smart-filter"
    And   I set the text "user5_fn" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And   I click on '[data-cy="text-search-filter-panel_search"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/accounts?firstname=lk_*user5_fn*"
    And   I expect the HTML element '[data-cy="item-row"]' to be visible
    And   I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="cell-firstname_{{ctx.user_id}}"]' to be visible

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 10 times on screen

    ## 103 Should go to detail page when click on account detail button
    When  I click on ".linid-smart-filter"
    And   I set the text "user5_fn" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And   I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen

    When I click on '[data-cy="see-button_{{ctx.user_id}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts/{{ctx.user_id}}"
    And I expect the HTML element '[data-cy="information-card--firstname"] [data-cy="value"]' contains "user5_fn"
    And I expect the HTML element '[data-cy="information-card--lastname"] [data-cy="value"]' contains "user5_ln"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "user5@example.com"
    And I expect the HTML element '[data-cy="information-card--createdBy"] [data-cy="value"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="information-card--insertDate"] [data-cy="value"]' contains "{{ctx.accountDate}}"

    ## 104 Should have pagination working
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
    And I expect the HTML element '[data-cy="item-row"]' appear 10 times on screen
    # Open the pagination dropdown
    When I click on 'input.q-select__focus-target'
    # Select the option to show 5 items per page
    And I click on '[role="listbox"] [role="option"]:first-child'
    Then I expect the HTML element '[data-cy="item-row"]' appear 5 times on screen
    # Go to the next page
    When I click on 'button[aria-label="Next page"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 5 times on screen
