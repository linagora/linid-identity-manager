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
    And I expect the HTML element '[data-cy="account-row"]' appear 10 times on screen
    And I expect the HTML element '[data-cy="cell-firstname"]' to be visible
    And I expect the HTML element '[data-cy="cell-lastname"]' to be visible
    And I expect the HTML element '[data-cy="cell-email"]' to be visible
    And I expect the HTML element '[data-cy="cell-createdBy"]' to be visible
    And I expect the HTML element '[data-cy="cell-insertDate"]' to be visible
    And I expect the HTML element '[data-cy="cell-actions"]' to be visible

    ## 102 Should filter accounts when using advanced search
    When I set the text "admin_fn" in the HTML element '[data-cy="field_firstname"]'
    And I set the text "admin_ln" in the HTML element '[data-cy="field_lastname"]'
    And I set the text "admin@example.com" in the HTML element '[data-cy="field_email"]'
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
    When I set the text "admin_fn" in the HTML element '[data-cy="field_firstname"]'
    And I click on '[data-cy="see-button"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/accounts/"
    And I expect the HTML element '[data-cy="information-card--firstname"] [data-cy="value"]' contains "admin_fn"
    And I expect the HTML element '[data-cy="information-card--lastname"] [data-cy="value"]' contains "admin_ln"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "admin@example.com"
    And I expect the HTML element '[data-cy="information-card--createdBy"] [data-cy="value"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="information-card--insertDate"] [data-cy="value"]' contains "{{ctx.accountDate}}"

    ## 104 Should have pagination working
    When I visit the "{{ env.E2E_FRONT_URL }}/accounts"
    Then I expect the HTML element '[data-cy="account-row"]' appear 10 times on screen
    # Open the pagination dropdown
    When I click on 'input.q-select__focus-target'
    # Select the option to show 5 items per page
    And I click on '[role="listbox"] [role="option"]:first-child'
    Then I expect the HTML element '[data-cy="account-row"]' appear 5 times on screen
    # Go to the next page
    When I click on 'button[aria-label="Next page"]'
    Then I expect the HTML element '[data-cy="account-row"]' appear 5 times on screen
