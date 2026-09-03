Feature: Test Account creation page

  ################## Account Creation ##################
  ## 101 Test page access and cancel button
  ## 102 Should display form fields and action buttons
  ## 103 Submitting an empty form should display required validation messages
  ## 104 validityPeriod.start invalidDate validation error
  ## 105 validityPeriod.start afterDate validation error
  ## 106 Submitting a valid form should create the account
  ## 107 Should display a success notification and redirect to the account details page
  ## 108 Remove the created account

  Scenario: Roundtrip about Account creation

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
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts"

    ## 101 Test page access and cancel button
    When  I click on '[data-cy="button_create"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/accounts/new"

    When  I click on '[data-cy="button_cancel"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
    When  I click on '[data-cy="button_create"]'

    ## 102 Should display form fields and action buttons
    Then I expect the HTML element '[data-cy="generic-creation-page"]' to be visible
    And  I expect the HTML element '[data-cy="generic-creation-page_title"]' contains "Création d'un compte"
    And  I expect the HTML element '[data-cy="field_externalId"]' to be visible
    And  I expect the HTML element '[data-cy="field_lastname"]' to be visible
    And  I expect the HTML element '[data-cy="field_firstname"]' to be visible
    And  I expect the HTML element '[data-cy="field_email"]' to be visible
    And  I expect the HTML element '[data-cy="field_validityPeriod.start"]' to be visible
    And  I expect the HTML element '[data-cy="field_organizationalUnit"]' to be visible
    And  I expect the HTML element '[data-cy="button_cancel"]' contains "Retour"
    And  I expect the HTML element '[data-cy="button_confirm"]' contains "Créer"

    ## 103 Submitting an empty form should display required validation messages
    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Ce champ est requis"

    ## 104 validityPeriod.start invalidDate validation error
    When I set the text "99/99/9999" in the HTML element '[data-cy="field_validityPeriod.start"]'
    And  I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Format de date invalide. Le format attendu est DD/MM/YYYY."

    ## 105 validityPeriod.start afterDate validation error
    When I set the text "01/01/2020" in the HTML element '[data-cy="field_validityPeriod.start"]'
    And  I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "La date ne peut pas être antérieure à la date du jour."

    ## 106 Submitting a valid form should create the account
    When I set the text "E2E_ACCOUNT_CREATION" in the HTML element '[data-cy="field_externalId"]'
    And  I set the text "AccountCreationE2E" in the HTML element '[data-cy="field_lastname"]'
    And  I set the text "TestE2E" in the HTML element '[data-cy="field_firstname"]'
    And  I set the text "e2e-account-creation@example.com" in the HTML element '[data-cy="field_email"]'
    And  I set the text "01/01/2100" in the HTML element '[data-cy="field_validityPeriod.start"]'
    And  I select '.q-menu .q-item:contains("root")' in '[data-cy="field_organizationalUnit"]'
    And  I click on '[data-cy="button_confirm"]'

    ## 107 Should display a success notification and redirect to the account details page
    Then I expect the HTML element ".q-notification__message" to be visible
    And  I expect the HTML element ".q-notification__message" contains "Compte créé avec succès."
    And  I expect current url matches "{{ env.E2E_FRONT_URL }}/accounts/.*"

    ## 108 Remove the created account (looked up by externalId)
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

    When  I request '{{env.E2E_API_URL}}/accounts?externalId=E2E_ACCOUNT_CREATION' with method 'GET'
    Then  I expect status code is 200
    And   I store 'accountId' as '{{response.body.content[0].id}}' in context
    When  I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then  I expect status code is 204
