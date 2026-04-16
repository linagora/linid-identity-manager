Feature: Test Account details page display

  ################## Account Details ##################
  ## 101 Should display all account information on detail page
  ## 102 Should display all action buttons on detail page
  ## 103 Edit button should go to the edit page
  ## 104 Cancel button should come back at accounts list page
  ## 105 Remove the account
  ## 106 Should display a not found notification when navigating to a non-existent account
  ## 107 Should display a generic error notification when navigating to an account with a malformed ID

  Scenario: Roundtrip about Account Details

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
    ################## Create account  #################
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
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "external-id1",
        "lastname": "Doe",
        "firstname": "John",
        "email": "john@example.com"
      }
      """
    Then I expect status code is 201
    And I store 'accountId' as '{{response.body.id}}' in context

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'GET'
    Then I expect status code is 200
    And I store 'createdBy' as '{{response.body.createdBy}}' in context
    And I store 'updatedBy' as '{{response.body.updatedBy}}' in context

    ####################################################
    ################## Account Details #################
    ####################################################

    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/{{ctx.accountId}}"

    ## 101 Should display all account information on detail page
    And I expect the HTML element '[data-cy="account-details-page_cards"]' to be visible
    And I expect the HTML element '[data-cy="information-card--firstname"] [data-cy="value"]' contains "John"
    And I expect the HTML element '[data-cy="information-card--lastname"] [data-cy="value"]' contains "Doe"
    And I expect the HTML element '[data-cy="information-card--email"] [data-cy="value"]' contains "john@example.com"
    And I expect the HTML element '[data-cy="information-card--createdBy"] [data-cy="value"]' contains "{{ctx.createdBy}}"
    And I expect the HTML element '[data-cy="information-card--updatedBy"] [data-cy="value"]' contains "{{ctx.updatedBy}}"
    And I expect the HTML element '[data-cy="information-card--insertDate"] [data-cy="value"]' to be visible
    And I expect the HTML element '[data-cy="information-card--updateDate"] [data-cy="value"]' to be visible

    ## 102 Should display all action buttons on detail page
    And I expect the HTML element '[data-cy="buttons-card"]' to be visible
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_cancel"]' contains "Retour"
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_edit"]' contains "Modifier"

    ## 103 Edit button should go to the edit page
    When I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts/{{ctx.accountId}}/edit"

    ## 104 Cancel button should come back at accounts list page
    When I visit the "{{ env.E2E_FRONT_URL }}/accounts/{{ctx.accountId}}"
    And I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"

    ## 105: Remove the account
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    ## 106 Should display a not found notification when navigating to a non-existent account
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-000000000000"
    Then I expect the HTML element '.q-notification__message' contains "Compte introuvable"
    And I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"

    ## 107 Should display a generic error notification when navigating to an account with a malformed ID
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/not-a-valid-uuid"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger le compte. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"
