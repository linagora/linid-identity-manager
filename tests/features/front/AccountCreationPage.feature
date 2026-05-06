Feature: Test Account creation page

  ################## Account Creation ##################
  ## 101 Should display title, form fields and action buttons
  ## 102 Submitting an empty form should display required validation messages
  ## 103 Submitting an invalid email should display email validation message
  ## 104 Submitting a form with a past validity start date should display date validation message
  ## 105 Submitting a valid form should create the account on the backend
  ## 106 Should display a success notification after creation
  ## 107 Should redirect to the account details page after creation
  ## 108 Remove the created account

  Scenario: Roundtrip about Account creation

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
    ################## Creation Page  ##################
    ####################################################

    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/create"

    ## 101 Should display title, form fields and action buttons
    Then I expect the HTML element '[data-cy="account-creation-page"]' to be visible
    And I expect the HTML element '[data-cy="account-creation-page_title"]' contains "Créer un nouveau compte"
    And I expect the HTML element '[data-cy="field_externalId"]' to be visible
    And I expect the HTML element '[data-cy="field_lastname"]' to be visible
    And I expect the HTML element '[data-cy="field_firstname"]' to be visible
    And I expect the HTML element '[data-cy="field_email"]' to be visible
    And I expect the HTML element '[data-cy="field_validityPeriodStart"]' to be visible
    And I expect the HTML element '[data-cy="button_cancel"]' contains "Annuler"
    And I expect the HTML element '[data-cy="button_confirm"]' contains "Créer"

    ## 102 Submitting an empty form should display required validation messages
    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="account-creation-page_form"]' contains "Ce champ est requis"
    And I expect current url is "{{ env.E2E_FRONT_URL }}/accounts/create"

    ## 103 Submitting an invalid email should display email validation message
    When I set the text "external-id-e2e" in the HTML element "[data-cy=\"field_externalId\"] input"
    And I set the text "Doe" in the HTML element "[data-cy=\"field_lastname\"] input"
    And I set the text "John" in the HTML element "[data-cy=\"field_firstname\"] input"
    And I set the text "not-an-email" in the HTML element "[data-cy=\"field_email\"] input"
    And I click on '[data-cy="account-creation-page_title"]'
    And I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="field_email"]' contains "Format d'e-mail invalide"
    And I expect current url is "{{ env.E2E_FRONT_URL }}/accounts/create"

    ## 104 Submitting a form with a past validity start date should display date validation message
    When I set the text "01/01/2020" in the HTML element "[data-cy=\"field_validityPeriodStart\"] input"
    And I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="field_validityPeriodStart"]' contains "La date ne peut pas être dans le passé"

    ## 105 Submitting a valid form should create the account on the backend
    When I set the text "john.doe@example.com" in the HTML element "[data-cy=\"field_email\"] input"
    And I set the text "01/01/2080" in the HTML element "[data-cy=\"field_validityPeriodStart\"] input"
    And I click on '[data-cy="button_confirm"]'

    ## 106 Should display a success notification after creation
    Then I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Compte créé avec succès"

    ## 107 Should redirect to the account details page after creation
    And I expect the HTML element '[data-cy="account-details-page_cards"]' to be visible

    ####################################################
    ################## Cleanup ##########################
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

    ## 108 Remove the created account (looked up by externalId)
    When I request '{{env.E2E_API_URL}}/accounts?externalId=external-id-e2e' with method 'GET'
    Then I expect status code is 200
    And I store 'accountId' as '{{response.body.content[0].id}}' in context
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204
