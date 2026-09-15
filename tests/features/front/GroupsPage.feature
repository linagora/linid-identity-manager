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
    ################## Create groups ###################
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

    When I request '{{env.E2E_API_URL}}/organizational-units?name=root&type=root' with method 'GET'
    Then I expect status code is 200
    And  I store 'rootId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I store 'linidId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-home-101",
        "name": "Group Home 101"
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "grp-home-102",
        "name": "Group Home 102",
        "parentId": "{{ctx.groupId}}",
        "description": "A group for the home page tests",
        "email": "grp-home-102@example.com",
        "organizationalUnitId": "{{ctx.rootId}}",
        "applicationId": "{{ctx.linidId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'groupId2' as '{{response.body.id}}' in context

    ####################################################
    ################## Group Homepage ##################
    ####################################################

    ## 101 Should display the group homepage with all groups listed
    When I click on '[data-cy="item_moduleGroupsPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups"
    And  I expect the HTML element '.linid-smart-filter' to be visible
    And  I expect the HTML element '.generic-entity-table' to be visible
    # Two rows: the parent group and the child group created above. No group is seeded.
    And  I expect the HTML element '[data-cy="item-row"]' appear 2 times on screen
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.groupId2}}"]' contains "grp-home-102"
    And  I expect the HTML element '[data-cy="cell-name_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-name_{{ctx.groupId2}}"]' contains "Group Home 102"
    And  I expect the HTML element '[data-cy="cell-parentName_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-parentName_{{ctx.groupId2}}"]' contains "Group Home 101"
    And  I expect the HTML element '[data-cy="cell-description_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-description_{{ctx.groupId2}}"]' contains "A group for the home page tests"
    And  I expect the HTML element '[data-cy="cell-organizationalUnitName_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-organizationalUnitName_{{ctx.groupId2}}"]' contains "root"
    And  I expect the HTML element '[data-cy="cell-applicationName_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-applicationName_{{ctx.groupId2}}"]' contains "LINID - Identity Manager"
    And  I expect the HTML element '[data-cy="cell-email_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-email_{{ctx.groupId2}}"]' contains "grp-home-102@example.com"
    And  I expect the HTML element '[data-cy="cell-createdBy_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-createdBy_{{ctx.groupId2}}"]' contains "admin_fn admin_ln"
    And  I expect the HTML element '[data-cy="cell-insertDate_{{ctx.groupId2}}"]' to be visible
    And  I expect the HTML element '[data-cy="cell-table_actions_{{ctx.groupId2}}"]' to be visible

    ## 102 Should filter groups when using advanced search
    When I click on ".linid-smart-filter"
    And  I set the text "unknown" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups?code=lk_*unknown*"
    And  I expect the HTML element '[data-cy="item-row"]' appear 0 times on screen

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 2 times on screen

    When I click on ".linid-smart-filter"
    And  I set the text "grp-home-102" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups?code=lk_*grp-home-102*"
    And  I expect the HTML element '[data-cy="item-row"]' to be visible
    And  I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.groupId2}}"]' to be visible

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 2 times on screen

    ## Cleanup
    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId2}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204
