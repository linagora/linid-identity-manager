Feature: Test Group creation page

  ################## Group Creation ##################
  ## 101 Test page access and cancel button
  ## 102 Should display title, form fields and action buttons
  ## 103 Submitting an empty form should display required validation messages
  ## 104 Should display the pattern validation message on an invalid code
  ## 105 Should display the email validation message on an invalid email
  ## 106 Submitting a valid form should create the group
  ## 107 Should display a success notification and redirect to the group list
  ## 108 Remove the created group

  Scenario: Roundtrip about Group creation

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
    When I click on '[data-cy="item_moduleGroupsPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups"

    ## 101 Test page access and cancel button
    When I click on '[data-cy="button_create"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups/new"

    When I click on '[data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups"
    When I click on '[data-cy="button_create"]'

    ## 102 Should display title, form fields and action buttons
    Then I expect the HTML element '[data-cy="generic-creation-page"]' to be visible
    And  I expect the HTML element '[data-cy="generic-creation-page_title"]' contains "Création d'un groupe"
    And  I expect the HTML element '[data-cy="field_code"]' to be visible
    And  I expect the HTML element '[data-cy="field_name"]' to be visible
    And  I expect the HTML element '[data-cy="field_parentId"]' to be visible
    And  I expect the HTML element '[data-cy="field_description"]' to be visible
    And  I expect the HTML element '[data-cy="field_organizationalUnitId"]' to be visible
    And  I expect the HTML element '[data-cy="field_applicationId"]' to be visible
    And  I expect the HTML element '[data-cy="field_email"]' to be visible
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

    ## 105 Should display the email validation message on an invalid email
    When I set the text "not-an-email" in the HTML element '[data-cy="field_email"]'
    And  I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Le format de l'email est invalide."

    ## 106 Submitting a valid form should create the group
    When I set the text "e2e-group-creation" in the HTML element '[data-cy="field_code"]'
    And  I set the text "E2E Group Creation" in the HTML element '[data-cy="field_name"]'
    And  I select '.q-menu .q-item:contains("IT Department")' in '[data-cy="field_parentId"]'
    And  I set the text "A group created from the creation page" in the HTML element '[data-cy="field_description"]'
    And  I select '.q-menu .q-item:contains("root")' in '[data-cy="field_organizationalUnitId"]'
    And  I select '.q-menu .q-item:contains("LINID - Identity Manager")' in '[data-cy="field_applicationId"]'
    And  I set the text "e2e-group-creation@example.com" in the HTML element '[data-cy="field_email"]'
    And  I click on '[data-cy="button_confirm"]'

    ## 107 Should display a success notification and redirect to the group details page
    Then I expect the HTML element ".q-notification__message" to be visible
    And  I expect the HTML element ".q-notification__message" contains "Groupe créé avec succès."
    And  I expect current url matches "{{ env.E2E_FRONT_URL }}/groups/.*"
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "E2E Group Creation"

    ## 108 Remove the created group (looked up by code)
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

    When I request '{{env.E2E_API_URL}}/groups?code=e2e-group-creation' with method 'GET'
    Then I expect status code is 200
    And  I store 'groupId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204
