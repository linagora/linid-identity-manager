Feature: Test Account homepage display

  ################## Account Homepage ##################
  ## 101 Should display the account homepage with all accounts listed
  ## 102 Should filter accounts when using advanced search
  ## 103 Should go to detail page when click on account detail button
  ## 104 Should have pagination working
  ## 105 Should display the Organizational Units tree

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
    When I click on '[data-cy="item_accounts"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/accounts"
    And I expect the HTML element '[data-cy="accounts-page_title"]' to be visible
    And I expect the HTML element '[data-cy="accounts-page_title"]' contains "Comptes"
    And I expect the HTML element '[data-cy="advanced-search-card"]' to be visible
    And I expect the HTML element '[data-cy="field_firstname"]' to be visible
    And I expect the HTML element '[data-cy="field_lastname"]' to be visible
    And I expect the HTML element '[data-cy="field_email"]' to be visible
    And I expect the HTML element '[data-cy="field_createdBy"]' to be visible
    And I expect the HTML element '[data-cy="field_insertDate"]' to be visible
    And I expect the HTML element '[data-cy="account-row"]' to be visible
    And I expect the HTML element '[data-cy="cell-firstname"]' to be visible
    And I expect the HTML element '[data-cy="cell-firstname"]' contains "admin_fn"
    And I expect the HTML element '[data-cy="cell-lastname"]' to be visible
    And I expect the HTML element '[data-cy="cell-lastname"]' contains "admin_ln"
    And I expect the HTML element '[data-cy="cell-email"]' to be visible
    And I expect the HTML element '[data-cy="cell-email"]' contains "admin@example.com"
    And I expect the HTML element '[data-cy="cell-createdBy"]' to be visible
    And I expect the HTML element '[data-cy="cell-createdBy"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="cell-insertDate"]' to be visible
    And I expect the HTML element '[data-cy="cell-insertDate"]' contains "{{ctx.accountDate}}"
    And I expect the HTML element '[data-cy="cell-actions"]' to be visible

    ## 102 Should filter accounts when using advanced search
    When I visit the '{{ env.E2E_FRONT_URL }}/accounts?node=00000000-0000-4000-8000-00000000eee1'
    And I set the text "user5_fn" in the HTML element '[data-cy="field_firstname"]'
    And I set the text "user5_ln" in the HTML element '[data-cy="field_lastname"]'
    And I set the text "user5@example.com" in the HTML element '[data-cy="field_email"]'
    And I set the text "admin_fn admin_ln" in the HTML element '[data-cy="field_createdBy"]'
    And I store the text of the HTML element '[data-cy="cell-insertDate"]' as 'accountDate' in context
    And I set the text "{{ctx.accountDate}}" in the HTML element '[data-cy="field_insertDate"]'
    Then I expect the HTML element '[data-cy="account-row"]' to be visible
    And I expect the HTML element '[data-cy="account-row"]' appear 1 times on screen
    When I clear the text in the HTML element '[data-cy="field_firstname"]'
    And I clear the text in the HTML element '[data-cy="field_lastname"]'
    And I clear the text in the HTML element '[data-cy="field_email"]'
    And I clear the text in the HTML element '[data-cy="field_createdBy"]'
    And I clear the text in the HTML element '[data-cy="field_insertDate"]'
    Then I expect the HTML element '[data-cy="account-row"]' appear 10 times on screen

    ## 103 Should go to detail page when click on account detail button
    When I set the text "user5_fn" in the HTML element '[data-cy="field_firstname"]'
    And I click on '[data-cy="see-button"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/accounts/"
    And I expect the HTML element '[data-cy="information-card--firstname"] [data-cy="value"]' contains "user5_fn"
    And I expect the HTML element '[data-cy="information-card--lastname"] [data-cy="value"]' contains "user5_ln"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "user5@example.com"
    And I expect the HTML element '[data-cy="information-card--createdBy"] [data-cy="value"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="information-card--insertDate"] [data-cy="value"]' contains "{{ctx.accountDate}}"

    ## 104 Should have pagination working
    When I visit the '{{ env.E2E_FRONT_URL }}/accounts?node=00000000-0000-4000-8000-00000000eee1'
    Then I expect the HTML element '[data-cy="account-row"]' appear 10 times on screen
    # Open the pagination dropdown
    When I click on 'input.q-select__focus-target'
    # Select the option to show 5 items per page
    And I click on '[role="listbox"] [role="option"]:first-child'
    Then I expect the HTML element '[data-cy="account-row"]' appear 5 times on screen
    # Go to the next page
    When I click on 'button[aria-label="Next page"]'
    Then I expect the HTML element '[data-cy="account-row"]' appear 5 times on screen

    ## 105 Should display the Organizational Units tree
    And I expect the HTML element '[data-cy="generic-tree"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000aa"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000bb"]' appear 1 times on screen
    When I click on '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000aa"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts?node=00000000-0000-4000-8000-0000000000aa"
    And I expect the HTML element '[data-cy="account-row"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="cell-firstname"]' contains "user1_fn"
    And I expect the HTML element '[data-cy="cell-lastname"]' contains "user1_ln"
    And I expect the HTML element '[data-cy="cell-email"]' contains "user1@example.com"
    And I expect the HTML element '[data-cy="cell-createdBy"]' contains "admin_fn admin_ln"
    When I click on '[data-cy="generic-tree-node-00000000-0000-4000-8000-0000000000bb"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts?node=00000000-0000-4000-8000-0000000000bb"
    And I expect the HTML element '[data-cy="cell-firstname"]' contains "user2_fn"
    And I expect the HTML element '[data-cy="cell-lastname"]' contains "user2_ln"
    And I expect the HTML element '[data-cy="cell-email"]' contains "user2@example.com"
    And I expect the HTML element '[data-cy="cell-createdBy"]' contains "admin_fn admin_ln"
