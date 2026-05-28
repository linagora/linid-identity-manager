Feature: Test Organizational Unit creation page

  ################## Organizational Unit Creation ##################
  ## 101 Should display title, form fields and action buttons
  ## 102 Parent organizational unit should be displayed with the resolved root name
  ## 103 Type select should expose the static types from the configuration
  ## 104 Submitting an empty form should display required validation messages
  ## 105 Submitting a valid form should create the organizational unit
  ## 106 Should display a success notification after creation
  ## 107 Visiting the page without a parent should redirect to the home page
  ## 108 Remove the created organizational unit

  Scenario: Roundtrip about Organizational Unit creation

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
    ################## Resolve root OU #################
    ####################################################

    Given I set http header 'Authorization' with '{{ env.E2E_AUTH_TOKEN }}'
    And I set http header 'Content-Type' with 'application/x-www-form-urlencoded'
    When I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=admin&password=password&scope=openid email profile roles
      """
    Then I expect status code is 200
    And I store 'accessToken' as '{{response.body.access_token}}' in context
    And I set http header 'Authorization' with 'Bearer {{ctx.accessToken}}'
    And I set http header 'Content-Type' with 'application/json'
    When I request '{{env.E2E_API_URL}}/organizational-units?name=root&type=root' with method 'GET'
    Then I expect status code is 200
    And I expect '{{ response.body.content.length }}' is "1"
    And I store 'rootID' as '{{response.body.content[0].id}}' in context

    ####################################################
    ################## Creation Page  ##################
    ####################################################

    Given I visit the "{{ env.E2E_FRONT_URL }}/organizational-units/create?parent={{ctx.rootID}}"

    ## 101 Should display title, form fields and action buttons
    Then I expect the HTML element '[data-cy="organizational-unit-creation-page"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-creation-page_title"]' contains "Créer une nouvelle unité organisationnelle"
    And I expect the HTML element '[data-cy="field_parent"]' to be visible
    And I expect the HTML element '[data-cy="field_name"]' to be visible
    And I expect the HTML element '[data-cy="field_type"]' to be visible
    And I expect the HTML element '[data-cy="button_cancel"]' contains "Annuler"
    And I expect the HTML element '[data-cy="button_confirm"]' contains "Créer"

    ## 102 Parent organizational unit should be displayed with the resolved root name
    And I expect the HTML element '[data-cy="field_parent"] input' to have value "root"

    ## 103 Type select should expose the static types from the configuration
    When I click on '[data-cy="field_type"] .q-select__focus-target'
    Then I expect the HTML element '[role="listbox"]' to be visible
    And I expect the HTML element '[role="listbox"]' contains "COMPANY"
    And I expect the HTML element '[role="listbox"]' contains "DEPARTMENT"
    And I expect the HTML element '[role="listbox"]' contains "TEAM"

    ## 104 Submitting an empty form should display required validation messages
    When I click on '[data-cy="organizational-unit-creation-page_title"]'
    And I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="organizational-unit-creation-page_form"]' contains "Ce champ est requis"
    And I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/create?parent={{ctx.rootID}}"

    ## 105 Submitting a valid form should create the organizational unit
    When I set the text "EngineeringE2E" in the HTML element "[data-cy=\"field_name\"] input"
    And I click on '[data-cy="field_type"] .q-select__focus-target'
    And I click on '[role="listbox"] [role="option"]:first-child'
    And I click on '[data-cy="button_confirm"]'

    ## 106 Should display a success notification after creation
    Then I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Unité organisationnelle créée avec succès"

    ####################################################
    ################## Missing parent ##################
    ####################################################

    ## 107 Visiting the page without a parent should redirect to the home page
    Given I visit the "{{ env.E2E_FRONT_URL }}/organizational-units/create"
    Then I expect the HTML element ".q-notification__message" contains "Une unité organisationnelle parente est requise."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/"

    ####################################################
    ################## Cleanup #########################
    ####################################################

    Given I set http header 'Authorization' with 'Bearer {{ctx.accessToken}}'
    And I set http header 'Content-Type' with 'application/json'

    ## 108 Remove the created organizational unit (looked up by name)
    When I request '{{env.E2E_API_URL}}/organizational-units?name=EngineeringE2E' with method 'GET'
    Then I expect status code is 200
    And I store 'organizationalUnitId' as '{{response.body.content[0].id}}' in context
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.organizationalUnitId}}' with method 'DELETE'
    Then I expect status code is 204
