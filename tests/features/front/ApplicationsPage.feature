Feature: Test Application homepage display

  ################## Application Homepage ##################
  ## 101 Should display the application homepage with all applications listed
  ## 102 Should filter applications when using advanced search
  ## 103 Should go to detail page when click on application detail button

  Scenario: Roundtrip about Application homepage

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
    ################## Create application ##############
    ####################################################

    Given I set http header 'Authorization' with '{{ env.E2E_AUTH_TOKEN }}'
    And   I set http header 'Content-Type' with 'application/x-www-form-urlencoded'
    When  I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=admin&password=password&scope=openid email profile roles
      """
    Then  I expect status code is 200
    And   I store 'accessToken' as '{{response.body.access_token}}' in context
    And   I set http header 'Authorization' with 'Bearer {{ctx.accessToken}}'
    And   I set http header 'Content-Type' with 'application/json'
    When  I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-home-101",
        "name": "Application Home 101",
        "description": "An application for the home page tests",
        "type": "OIDC",
        "claimsTemplate": "{ \"sub\": \"id\" }"
      }
      """
    Then  I expect status code is 201
    And   I store 'applicationId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-home-102",
        "name": "Application Home 102",
        "description": "An application 2 for the home page tests",
        "type": "OIDC",
        "claimsTemplate": "{ \"sub\": \"id\" }"
      }
      """
    Then I expect status code is 201
    And  I store 'applicationId2' as '{{response.body.id}}' in context

    ####################################################
    ################## Application Homepage ################
    ####################################################

    ## 101 Should display the application homepage with all applications listed
    When I click on '[data-cy="item_moduleApplicationsPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications"
    And  I expect the HTML element '.linid-smart-filter' to be visible
    And  I expect the HTML element '.generic-entity-table' to be visible
    And  I expect the HTML element '[data-cy="item-row"]' appear 2 times on screen
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.applicationId}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.applicationId}}"]' contains "app-home-101"
    And  I expect the HTML element '[data-cy="cell-name_{{ctx.applicationId}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-name_{{ctx.applicationId}}"]' contains "Application Home 101"
    And  I expect the HTML element '[data-cy="cell-description_{{ctx.applicationId}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-description_{{ctx.applicationId}}"]' contains "An application for the home page tests"
    And  I expect the HTML element '[data-cy="cell-type_{{ctx.applicationId}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-type_{{ctx.applicationId}}"]' contains "OIDC"
    And  I expect the HTML element '[data-cy="cell-createdBy_{{ctx.applicationId}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-createdBy_{{ctx.applicationId}}"]' contains "admin_fn admin_ln"
    And  I expect the HTML element '[data-cy="cell-insertDate_{{ctx.applicationId}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-table_actions_{{ctx.applicationId}}"]' to be visible

    ## 102 Should filter applications when using advanced search
    When I click on ".linid-smart-filter"
    And  I set the text "unknown" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications?code=lk_*unknown*"
    And  I expect the HTML element '[data-cy="item-row"]' appear 0 times on screen

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 2 times on screen

    When I click on ".linid-smart-filter"
    And  I set the text "app-home-102" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications?code=lk_*app-home-102*"
    And  I expect the HTML element '[data-cy="item-row"]' to be visible
    And  I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.applicationId2}}"]' to be visible

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 2 times on screen

    ## 103 Should go to detail page when click on application detail button
    When I click on '[data-cy="see-button_{{ctx.applicationId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"

    ## Cleanup
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId2}}' with method 'DELETE'
    Then I expect status code is 204
