Feature: Test Account homepage display

  ################## Account Homepage ##################
  ## 101 Should display the account homepage with all accounts listed
  ## 102 Should filter accounts when using advanced search
  ## 103 Should go to detail page when click on account detail button
  ## 104 Should have pagination working
  ## 105 Should clear all active filters at once with the clear button
  ## 106 Should rename a favorite filter set from its rename button
  ## 107 Should override a favorite filter set from its own override button
  ## 108 Should disable the favorite override button when no filter is active

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

    ## 105 Should clear all active filters at once with the clear button
    # Reset the query, no filter is active yet so the clear button is not rendered
    When I click on '[data-cy="item_moduleAccountsPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
    And I expect the HTML element '[data-cy="linid-smart-filter-clear"]' not exists

    # Apply a first filter on firstname: the clear button becomes available
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I click on '[data-cy="linid-filter-panel_item-firstname"]'
    And I set the text "user5_fn" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts?firstname=lk_*user5_fn*"
    And I expect the HTML element '[data-cy="linid-smart-filter-chips"]' to be visible
    And I expect the HTML element '[data-cy="linid-smart-filter-clear"]' to be visible

    # Apply a second filter on lastname: both filters now shape the query
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I click on '[data-cy="linid-filter-panel_item-lastname"]'
    And I set the text "user5_ln" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url contains "firstname=lk_*user5_fn*"
    And I expect current url contains "lastname=lk_*user5_ln*"

    # Clear every filter at once: the query, the chips and the clear button are reset
    When I click on '[data-cy="linid-smart-filter-clear"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
    And I expect the HTML element '[data-cy="linid-smart-filter-chips"]' not exists
    And I expect the HTML element '[data-cy="linid-smart-filter-clear"]' not exists

    ## 106 Should rename a favorite filter set from its rename button
    # Apply a filter, then create a favorite out of the current search
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I click on '[data-cy="linid-filter-panel_item-firstname"]'
    And I set the text "user5_fn" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    And I click on '[data-cy="linid-smart-filter-field"]'
    And I click on '[data-cy="linid-smart-favorite-panel"] [data-cy="button_create"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "MyFavorite" in the HTML element '[data-cy="field_favoriteName"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element '[data-cy="favorite-label_0"]' contains "MyFavorite"

    # Rename that favorite through its own rename button
    When I click on '[data-cy="button_rename_0"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Renommer un favori?"
    When I set the text "RenamedFavorite" in the HTML element '[data-cy="field_favoriteName"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" contains "RenamedFavorite"
    And I expect the HTML element ".q-dialog__backdrop" not exists
    And I expect the HTML element '[data-cy="favorite-label_0"]' contains "RenamedFavorite"

    ## 107 Should override a favorite filter set from its own override button
    # Apply the favorite: it still carries its original firstname search
    When I click on '[data-cy="favorite-label_0"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts?firstname=lk_*user5_fn*"

    # Close the still-open menu by toggling the field, then replace the search with a lastname filter
    When I click on '[data-cy="linid-smart-filter-field"]'
    Then I expect the HTML element '[data-cy="linid-smart-filter-menu"]' not exists
    When I click on '[data-cy="linid-smart-filter-clear"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I click on '[data-cy="linid-filter-panel_item-lastname"]'
    And I set the text "user6_ln" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts?lastname=lk_*user6_ln*"

    # Override the favorite with the current search through its own override button
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I click on '[data-cy="button_override_0"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And I expect the HTML element ".q-notification__message" contains "RenamedFavorite"
    And I expect the HTML element ".q-dialog__backdrop" not exists

    # Apply the favorite again: it now restores the overridden search (lastname, not firstname)
    When I click on '[data-cy="favorite-label_0"]'
    Then I expect current url contains "lastname=lk_*user6_ln*"
    And I expect the current URL no longer contains "firstname"

    ## 108 Should disable the favorite override button when no filter is active
    # The menu is still open from the previous apply; clear the active filter
    When I click on '[data-cy="linid-smart-filter-clear"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
    # With no active filter, the favorite's own override button is disabled
    When I click on '[data-cy="linid-smart-filter-field"]'
    Then I expect the HTML element '[data-cy="button_override_0"]' to be disabled
