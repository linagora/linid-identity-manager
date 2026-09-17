Feature: Test Role creation page

  ################## Role Creation ##################
  ## 101 Test page access and cancel button
  ## 102 Should display title, form fields and action buttons
  ## 103 Submitting an empty form should display required validation messages
  ## 104 Should display the pattern validation message on an invalid code
  ## 105 Submitting a valid form should create the role
  ## 106 Should display a success notification and redirect to the role details page
  ## 107 Remove the created role

  Scenario: Roundtrip about Role creation

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
    When I click on '[data-cy="item_moduleRolesPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles"

    ## 101 Test page access and cancel button
    When I click on '[data-cy="button_create"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles/new"

    When I click on '[data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles"
    When I click on '[data-cy="button_create"]'

    ## 102 Should display title, form fields and action buttons
    Then I expect the HTML element '[data-cy="generic-creation-page"]' to be visible
    And  I expect the HTML element '[data-cy="generic-creation-page_title"]' contains "Création d'un rôle"
    And  I expect the HTML element '[data-cy="field_code"]' to be visible
    And  I expect the HTML element '[data-cy="field_name"]' to be visible
    And  I expect the HTML element '[data-cy="field_description"]' to be visible
    And  I expect the HTML element '[data-cy="button_cancel"]' contains "Retour"
    And  I expect the HTML element '[data-cy="button_confirm"]' contains "Créer"

    ## 103 Submitting an empty form should display required validation messages
    When I click on '[data-cy="generic-creation-page_title"]'
    And  I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Ce champ est requis"

    ## 104 Should display the pattern validation message on an invalid code
    When I set the text "invalid code!" in the HTML element '[data-cy="field_code"]'
    And  I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Seuls les lettres, les chiffres, les tirets et les tirets bas sont autorisés."

    ## 105 Submitting a valid form should create the role
    When I set the text "e2e-role-creation" in the HTML element '[data-cy="field_code"]'
    And  I set the text "E2E Role Creation" in the HTML element '[data-cy="field_name"]'
    And  I set the text "A role created from the creation page" in the HTML element '[data-cy="field_description"]'
    And  I click on '[data-cy="button_confirm"]'

    ## 106 Should display a success notification and redirect to the role details page
    Then I expect the HTML element ".q-notification__message" to be visible
    And  I expect the HTML element ".q-notification__message" contains "Rôle créé avec succès."
    And  I expect current url matches "{{ env.E2E_FRONT_URL }}/roles/.*"
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "E2E Role Creation"

    ## 107 Remove the created role (looked up by code)
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

    When I request '{{env.E2E_API_URL}}/roles?code=e2e-role-creation' with method 'GET'
    Then I expect status code is 200
    And  I store 'roleId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/roles/{{ctx.roleId}}' with method 'DELETE'
    Then I expect status code is 204
