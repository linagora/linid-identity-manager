Feature: Test Account details page display

  ################## Account Details ##################
  ## 101 Should display all account information on detail page
  ## 102 Should display all action buttons on detail page
  ## 103 Cancel button should come back at accounts list page
  ## 104 Remove the account
  ## 105 Should display a not found notification when navigating to a non-existent account
  ## 106 Should display a generic error notification when navigating to an account with a malformed ID
  ## 107 Lifecycle case 1 - INACTIVE, future validity start
  ## 108 Lifecycle case 2 - INACTIVE, not activated yet
  ## 109 Lifecycle case 3 - ACTIVE, no end date, no suspension
  ## 110 Lifecycle case 4 - ACTIVE, end > 15 days, no suspension
  ## 111 Lifecycle case 5 - ACTIVE, end <= 15 days, no suspension
  ## 112 Lifecycle case 6 - ACTIVE, no end date, suspension planned
  ## 113 Lifecycle case 7 - ACTIVE, end > 15 days, suspension planned
  ## 114 Lifecycle case 8 - ACTIVE, end <= 15 days, suspension planned
  ## 115 Lifecycle case 9 - SUSPENDED, no validity end, no suspension end
  ## 116 Lifecycle case 10 - SUSPENDED, no validity end, suspension with end
  ## 117 Lifecycle case 11 - SUSPENDED, end > 15 days
  ## 118 Lifecycle case 12 - SUSPENDED, end <= 15 days
  ## 119 Immediate activation - dialog opens correctly
  ## 120 Immediate activation - cancel button closes dialog
  ## 121 Immediate activation - success, account status updated after form submission
  ## 122 Scheduled activation - dialog opens correctly
  ## 123 Scheduled activation - cancel button closes dialog
  ## 124 Scheduled activation - validityPeriodStart invalidDate validation error
  ## 125 Scheduled activation - validityPeriodStart afterDate validation error
  ## 126 Scheduled activation - success, account status updated after form submission

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
        "email": "john@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        }
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

    ## 103 Cancel button should come back at accounts list page
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"

    ## 104: Remove the account
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.accountId}}' with method 'DELETE'
    Then I expect status code is 204

    ## 105 Should display a not found notification when navigating to a non-existent account
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-000000000000"
    Then I expect the HTML element '.q-notification__message' contains "Compte introuvable"
    And I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"

    ## 106 Should display a generic error notification when navigating to an account with a malformed ID
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/not-a-valid-uuid"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger le compte. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/accounts"

    ## 107 Lifecycle case 1 - INACTIVE, future validity start
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c1"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_inactive"]' contains "Inactif"
    And I expect the HTML element '[data-cy="account-not-activated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-status-badge_active"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-banner"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 108 Lifecycle case 2 - INACTIVE, not activated yet
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c2"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_inactive"]' contains "Inactif"
    And I expect the HTML element '[data-cy="account-not-activated-info-text"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-banner"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 109 Lifecycle case 3 - ACTIVE, no end date, no suspension
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c3"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' contains "Actif"
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-banner"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 110 Lifecycle case 4 - ACTIVE, end > 15 days, no suspension
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c4"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' contains "Actif"
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 111 Lifecycle case 5 - ACTIVE, end <= 15 days, no suspension
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c5"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' contains "Actif"
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 112 Lifecycle case 6 - ACTIVE, no end date, suspension planned
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c6"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' contains "Actif"
    And I expect the HTML element '[data-cy="account-suspended-info-text"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-banner"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 113 Lifecycle case 7 - ACTIVE, end > 15 days, suspension planned
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c7"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' contains "Actif"
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' to be visible
    And I expect the HTML element '[data-cy="account-suspended-info-text"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-banner"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 114 Lifecycle case 8 - ACTIVE, end <= 15 days, suspension planned
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c8"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' contains "Actif"
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' to be visible
    And I expect the HTML element '[data-cy="account-suspended-info-text"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-suspended-banner"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 115 Lifecycle case 9 - SUSPENDED, no validity end, no suspension end
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-0000000000c9"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-suspended-banner"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' not exists
    And I expect the HTML element '[data-cy="account-status-badge_inactive"]' not exists
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' not exists
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 116 Lifecycle case 10 - SUSPENDED, no validity end, suspension with end
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-00000000000a"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-suspended-banner"]' to be visible
    And I expect the HTML element '[data-cy="account-status-badge_active"]' not exists
    And I expect the HTML element '[data-cy="account-status-badge_inactive"]' not exists
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' not exists
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 117 Lifecycle case 11 - SUSPENDED, end > 15 days
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-00000000000b"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-suspended-banner"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' not exists
    And I expect the HTML element '[data-cy="account-status-badge_active"]' not exists
    And I expect the HTML element '[data-cy="account-status-badge_inactive"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 118 Lifecycle case 12 - SUSPENDED, end <= 15 days
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-0000-0000-00000000000c"
    Then I expect the HTML element '[data-cy="account-details-page_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="account-suspended-banner"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-warning-banner"]' to be visible
    And I expect the HTML element '[data-cy="account-deactivated-info-text"]' not exists
    And I expect the HTML element '[data-cy="account-status-badge_active"]' not exists
    And I expect the HTML element '[data-cy="account-status-badge_inactive"]' not exists
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible

    ## 119 Immediate activation - dialog opens correctly
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-4000-8000-0000000000d1"
    Then I expect the HTML element '[data-cy="account-details-page"]' to be visible
    And I expect the HTML element '[data-cy="account-details-page_title"]' to be visible
    And I expect the HTML element '[data-cy="account-details-page_cards"]' to be visible
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible
    And I expect the HTML element '[data-cy="account-activation-actions"]' to be visible
    And I expect the HTML element '[data-cy="account-activation-actions"]' contains "Activation"
    When I click on '[data-cy="account-activation-actions"]'
    Then I expect the HTML element '[data-cy="dropdown-button_item_activation.immediate"]' to be visible
    When I click on '[data-cy="dropdown-button_item_activation.immediate"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    And I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Activation immédiate du compte"
    And I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Êtes-vous sûr de vouloir activer ce compte immédiatement ?"
    And I expect the HTML element '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]' contains "Annuler"
    And I expect the HTML element '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]' contains "Activer"

    ## 120 Immediate activation - cancel button closes dialog
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists

    ## 121 Immediate activation - success, account status updated after form submission
    When I click on '[data-cy="account-activation-actions"]'
    And I click on '[data-cy="dropdown-button_item_activation.immediate"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Le compte pourra être activé dans une heure"

    ## 122 Scheduled activation - dialog opens correctly
    Given I visit the "{{ env.E2E_FRONT_URL }}/accounts/00000000-0000-4000-8000-0000000000d5"
    Then I expect the HTML element '[data-cy="account-details-page"]' to be visible
    And I expect the HTML element '[data-cy="account-details-page_title"]' to be visible
    And I expect the HTML element '[data-cy="account-details-page_cards"]' to be visible
    And I expect the HTML element '[data-cy="account-lifecycle-actions"]' to be visible
    And I expect the HTML element '[data-cy="account-activation-actions"]' to be visible
    And I expect the HTML element '[data-cy="account-activation-actions"]' contains "Activation"
    When I click on '[data-cy="account-activation-actions"]'
    Then I expect the HTML element '[data-cy="dropdown-button_item_activation.scheduled"]' to be visible
    When I click on '[data-cy="dropdown-button_item_activation.scheduled"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Planifier l'activation du compte"
    And I expect the HTML element '[data-cy="form-dialog_content"]' contains "Veuillez sélectionner une date d'activation."
    And I expect the HTML element '[data-cy="form-dialog_field-container_validityPeriodStart"]' contains "Date d'activation"
    And I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    And I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Planifier"

    ## 123 Scheduled activation - cancel button closes dialog
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 124 Scheduled activation - validityPeriodStart invalidDate validation error
    When I click on '[data-cy="account-activation-actions"]'
    And I click on '[data-cy="dropdown-button_item_activation.scheduled"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "99/99/9999" in the HTML element '[data-cy="field_validityPeriodStart"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_validityPeriodStart"]' contains "Format de date invalide. Le format attendu est DD/MM/YYYY."

    ## 125 Scheduled activation - validityPeriodStart afterDate validation error
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    And I click on '[data-cy="account-activation-actions"]'
    And I click on '[data-cy="dropdown-button_item_activation.scheduled"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "01/01/2020" in the HTML element '[data-cy="field_validityPeriodStart"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_validityPeriodStart"]' contains "La date ne peut pas être antérieure à la date du jour."

    ## 126 Scheduled activation - success, account status updated after form submission
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    And I click on '[data-cy="account-activation-actions"]'
    And I click on '[data-cy="dropdown-button_item_activation.scheduled"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "01/01/2100" in the HTML element '[data-cy="field_validityPeriodStart"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Le compte pourra être activé à partir du 01/01/2100"

