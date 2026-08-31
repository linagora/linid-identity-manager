Feature: Test Application creation page

  ################## Application Creation ##################
  ## 101 Test page access and cancel button
  ## 102 Should display title, form fields and action buttons
  ## 103 Submitting an empty form should display required validation messages
  ## 104 Submitting a valid form should create the application
  ## 105 Should display a success notification after creation
  ## 106 Remove the created application

  Scenario: Roundtrip about Application creation

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
    ################## Creation Page  ##################
    ####################################################
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications"

    ## 101 Test page access and cancel button
    When  I click on '[data-cy="button_create"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/applications/new"

    When  I click on '[data-cy="button_cancel"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/applications"
    When  I click on '[data-cy="button_create"]'

    ## 102 Should display title, form fields and action buttons
    Then I expect the HTML element '[data-cy="generic-creation-page"]' to be visible
    And  I expect the HTML element '[data-cy="generic-creation-page_title"]' contains "Création d'une application"
    And  I expect the HTML element '[data-cy="field_code"]' to be visible
    And  I expect the HTML element '[data-cy="field_name"]' to be visible
    And  I expect the HTML element '[data-cy="field_type"]' to be visible
    And  I expect the HTML element '[data-cy="field_domain"]' to be visible
    And  I expect the HTML element '[data-cy="field_description"]' to be visible
    And  I expect the HTML element '[data-cy="field_claimsTemplate"]' to be visible
    And  I expect the HTML element '[data-cy="button_cancel"]' contains "Retour"
    And  I expect the HTML element '[data-cy="button_confirm"]' contains "Créer"

    ## 103 Submitting an empty form should display required validation messages
    When I click on '[data-cy="generic-creation-page_title"]'
    And  I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Ce champ est requis"

    ## 104 Submitting a valid form should create the application
    When I set the text "TEST_APP" in the HTML element "input[data-cy=\"field_code\"]"
    And  I set the text "Test application" in the HTML element "input[data-cy=\"field_name\"]"
    And  I set the text "test" in the HTML element "input[data-cy=\"field_type\"]"
    And  I set the text "Test application domain" in the HTML element "input[data-cy=\"field_domain\"]"
    And  I set the text "Test application description" in the HTML element "textarea[data-cy=\"field_description\"]"
    And  I set the text "{}" in the HTML element "textarea[data-cy=\"field_claimsTemplate\"]"
    And  I click on '[data-cy="button_confirm"]'

    ## 105 Should display a success notification after creation
    Then I expect the HTML element ".q-notification__message" to be visible
    And  I expect the HTML element ".q-notification__message" contains "Application créée avec succès"
    And  I expect current url matches "{{ env.E2E_FRONT_URL }}/applications/.*"

    ## 106 Remove the created application (looked up by name)
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

    When  I request '{{env.E2E_API_URL}}/applications?code=TEST_APP' with method 'GET'
    Then  I expect status code is 200
    And   I store 'applicationID' as '{{response.body.content[0].id}}' in context
    When  I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationID}}' with method 'DELETE'
    Then  I expect status code is 204
