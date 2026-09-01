Feature: Test Organizational Unit details panel display

  ## 101 Should reach the details page from the table and display title, badge and OU information for an active OU
  ## 102 Should display the suspension dropdown for an active OU
  ## 103 Immediate suspension should suspend the OU once confirmed
  ## 104 Schedule suspension should open the form dialog with date and reason fields
  ## 105 Schedule suspension should succeed when localized dates are submitted
  ## 106 Should display the suspended banner and badge for a currently suspended OU
  ## 107 Edit suspension end from the banner should open the form dialog
  ## 108 Edit suspension end should succeed when a localized date is submitted
  ## 109 Immediate reactivation from the banner should end the suspension once confirmed
  ## 110 Visiting an unknown organizational unit should display a load-error notification
  ## 111 Create child button should open the creation page with the current OU as parent
  ## 112 Edit the organizational unit from the profile panel
  ## 201 Should display an empty accounts card for an organizational unit without accounts
  ## 202 Should display the accounts card with the attached account
  ## 203 Attach dialog should close on cancel without attaching an account
  ## 204 Should attach an existing account from the dialog
  ## 205 Should detach an account after confirmation

  Scenario: Roundtrip about Organizational Unit details

    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}'
    When I set the text "admin" in the HTML element "input#userfield"
    And I set the text "password" in the HTML element "input#passwordfield"
    And I click on "button.btn-success"
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/"

    ## 101 Should reach the details page from the table and display title, badge and OU information for an active OU
    Given I store "company_a_id" as "00000000-0000-4000-8000-00000000000a" in context
    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    And I expect the HTML element '[data-cy="generic-entity-table"]' to be visible
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "Company A" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="see-button_{{ctx.company_a_id}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.company_a_id}}"
    And I expect the HTML element '[data-cy="generic-details-page"]' to be visible
    And I expect the HTML element '[data-cy="entity-profile-panel"]' to be visible
    And I expect the HTML element '[data-cy="entity-profile-panel_status-badge"]' contains "Actif"
    And I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "Company A"
    And I expect the HTML element '[data-cy="information-card--createdBy"] [data-cy="value"]' to be visible
    And I expect the HTML element '[data-cy="details-section_lifecycle"]' to be visible
    And I expect the HTML element '[data-cy="information-card--insertDate"] [data-cy="value"]' to be visible
    And I expect the HTML element '[data-cy="entity-profile-panel_back-button"]' contains "Liste des unités organisationnelles"

    ## 102 Should display the suspension dropdown for an active OU
    And I expect the HTML element '[data-cy="organizational-unit-suspension-actions"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-suspended-banner"]' not exists

    ## 103 Immediate suspension should suspend the OU once confirmed
    When I click on '[data-cy="organizational-unit-suspension-actions"]'
    And I click on '[data-cy="dropdown-button_item_suspension.immediate"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Suspendre l'unité organisationnelle"
    And I expect the HTML element '[data-cy="form-dialog_field-container_reason"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_field-container_subreason"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_field-container_comment"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    When I click on '[data-cy="organizational-unit-suspension-actions"]'
    And I click on '[data-cy="dropdown-button_item_suspension.immediate"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I select '.q-menu .q-item:contains("Suspension Reason A")' in '[data-cy="field_reason"]'
    And I select '.q-menu .q-item:contains("Suspension Sub-reason A.1")' in '[data-cy="field_subreason"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Unité organisationnelle suspendue avec succès."

    ## 104 Schedule suspension should open the form dialog with date and reason fields
    When I click on '[data-cy="organizational-unit-suspension-actions"]'
    And I click on '[data-cy="dropdown-button_item_suspension.scheduled"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Planifier une suspension"
    And I expect the HTML element '[data-cy="form-dialog_field-container_start"]' to be visible
    And I expect the HTML element '[data-cy="field_start"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_field-container_end"]' to be visible
    And I expect the HTML element '[data-cy="field_end"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_field-container_reason"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_field-container_subreason"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_field-container_comment"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 105 Schedule suspension should succeed when localized dates are submitted
    When I click on '[data-cy="organizational-unit-suspension-actions"]'
    And I click on '[data-cy="dropdown-button_item_suspension.scheduled"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "01/01/2100" in the HTML element '[data-cy="field_start"]'
    And I set the text "31/12/2100" in the HTML element '[data-cy="field_end"]'
    And I select '.q-menu .q-item:contains("Suspension Reason A")' in '[data-cy="field_reason"]'
    And I select '.q-menu .q-item:contains("Suspension Sub-reason A.1")' in '[data-cy="field_subreason"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Suspension planifiée avec succès."
    And I expect the HTML element '[data-cy="entity-profile-panel_status-badge"]' contains "Actif"
    And I expect the HTML element '[data-cy="organizational-unit-suspended-info-text"]' to be visible

    ## 106 Should display the suspended banner and badge for a currently suspended OU
    Given I store "suspended_ou_no_end_id" as "00000000-0000-4000-8000-0000000000e1" in context
    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "SuspendedOuNoEnd" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="see-button_{{ctx.suspended_ou_no_end_id}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.suspended_ou_no_end_id}}"
    And I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "SuspendedOuNoEnd"
    And I expect the HTML element '[data-cy="entity-profile-panel_status-badge"]' contains "Suspendu"
    And I expect the HTML element '[data-cy="organizational-unit-suspended-banner"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-activation-actions"]' not exists
    And I expect the HTML element '[data-cy="organizational-unit-suspension-actions"]' not exists

    ## 107 Edit suspension end from the banner should open the form dialog
    When I click on '[data-cy="organizational-unit-suspended-banner_modify-suspension-end-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Modifier la fin de suspension"
    And I expect the HTML element '[data-cy="form-dialog_field-container_end"]' to be visible
    And I expect the HTML element '[data-cy="field_end"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 108 Edit suspension end should succeed when a localized date is submitted
    When I click on '[data-cy="organizational-unit-suspended-banner_modify-suspension-end-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "31/12/2100" in the HTML element '[data-cy="field_end"]'
    And I select '.q-menu .q-item:contains("Suspension Reason A")' in '[data-cy="field_reason"]'
    And I select '.q-menu .q-item:contains("Suspension Sub-reason A.1")' in '[data-cy="field_subreason"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Date de fin de suspension mise à jour avec succès."

    ## 109 Immediate reactivation from the banner should end the suspension once confirmed
    When I click on '[data-cy="organizational-unit-suspended-banner_clear-suspension-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Réactiver l'unité organisationnelle"
    And I expect the HTML element '[data-cy="form-dialog_field-container_comment"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    When I click on '[data-cy="organizational-unit-suspended-banner_clear-suspension-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "Réactivation e2e" in the HTML element '[data-cy="field_comment"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "L'unité organisationnelle a été réactivée avec succès."
    And I expect the HTML element '[data-cy="organizational-unit-suspended-banner"]' not exists
    And I expect the HTML element '[data-cy="entity-profile-panel_status-badge"]' contains "Actif"

    ## 110 Visiting an unknown organizational unit should display a load-error notification
    Given I visit the "{{ env.E2E_FRONT_URL }}/organizational-units/00000000-0000-4000-8000-deadbeefdead"
    Then I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Impossible de charger l'unité organisationnelle. Veuillez réessayer plus tard."
    And I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"

    ## 111 Create child button should open the creation page with the current OU as parent
    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "Company A" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="see-button_{{ctx.company_a_id}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.company_a_id}}"
    And I expect the HTML element '[data-cy="organizational-unit-create-child-button"]' to be visible
    When I click on '[data-cy="organizational-unit-create-child-button"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/create?parent={{ctx.company_a_id}}"
    And I expect the HTML element '[data-cy="organizational-unit-creation-page"]' to be visible
    And I expect the HTML element '[data-cy="field_parent"] input' to have value "Company A"

    ## 112 Edit the organizational unit from the profile panel
    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "Dept B1-1" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="see-button_00000000-0000-4000-8000-0000000000c1"]'
    Then I expect the HTML element '[data-cy="entity-profile-panel"]' to be visible
    And I expect the HTML element '[data-cy="entity-profile-panel_edit-button"]' to be visible
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "DEPARTMENT"

    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog"]' contains "Modifier Dept B1-1"
    And I expect the HTML element '[data-cy="field_name"]' to have value "Dept B1-1"
    And I expect the HTML element '[data-cy="field_type"]' contains "DEPARTMENT"

    # Cancelling the dialog must not change anything
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "Dept B1-1"

    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible

    # Clearing a required field displays the validation message
    When I clear the text in the HTML element '[data-cy="field_name"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' contains "Ce champ est requis."

    When I set the text "Dept B1-1 edited" in the HTML element '[data-cy="field_name"]'
    And I select '.q-menu .q-item:contains("TEAM")' in '[data-cy="field_type"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" contains "L'unité organisationnelle a été mise à jour avec succès."
    And I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "Dept B1-1 edited"
    And I expect the HTML element '[data-cy="information-card--type"] [data-cy="value"]' contains "TEAM"

    ## 201 Should display an empty accounts card for an organizational unit without accounts
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
    And  I store 'rootID' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {
        "parent": "{{ctx.rootID}}",
        "name": "OU Accounts E2E",
        "type": "test",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'ouAccountsId' as '{{response.body.id}}' in context

    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "OU Accounts E2E" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="see-button_{{ctx.ouAccountsId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.ouAccountsId}}"
    And I expect the HTML element '[data-cy="generic-editable-table-card"]' to be visible
    And I expect the HTML element '[data-cy="generic-editable-table-card_title"]' contains "Comptes"
    And I expect the HTML element '[data-cy="generic-editable-table-card_add-button"]' contains "Attacher un compte"
    And I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Aucun compte rattaché à cette unité organisationnelle."

    ## 202 Should display the accounts card with the attached account
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-ui-1",
        "lastname": "Martin",
        "firstname": "Paul",
        "email": "paul-oua@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.ouAccountsId}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'attachedAccountId' as '{{response.body.id}}' in context

    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "OU Accounts E2E" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="see-button_{{ctx.ouAccountsId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.ouAccountsId}}"
    And I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Martin"
    And I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Paul"
    And I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "paul-oua@example.com"
    And I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' not contains "Aucun compte rattaché à cette unité organisationnelle."
    And I expect the HTML element '[data-cy="edit-button_{{ctx.attachedAccountId}}"]' not exists
    And I expect the HTML element '[data-cy="delete-button_{{ctx.attachedAccountId}}"]' contains "Détacher"

    ## 203 Attach dialog should close on cancel without attaching an account
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog_title"]' contains "Attacher un compte"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_accountId"]' contains "Compte"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Attacher"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 204 Should attach an existing account from the dialog
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-oua-ui-2",
        "lastname": "Durand",
        "firstname": "Alice",
        "email": "alice-oua@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootID}}",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'secondAccountId' as '{{response.body.id}}' in context

    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    # The account list is virtualized: a first scroll renders the end of the loaded slice and
    # extends it, a second one reaches the newly rendered last options (one scroll is not enough)
    When I click on '[data-cy="field_accountId"]'
    And  I scroll to 'bottom' into '.q-menu'
    And  I scroll to 'bottom' into '.q-menu'
    And  I click on '.q-menu .q-item:contains("Durand Alice")'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Compte attaché avec succès."
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Durand"
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "alice-oua@example.com"

    ## 205 Should detach an account after confirmation
    When I click on '[data-cy="delete-button_{{ctx.secondAccountId}}"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    And  I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Détacher le compte Durand Alice"
    And  I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Voulez-vous vraiment détacher le compte Durand Alice"
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Durand"
    When I click on '[data-cy="delete-button_{{ctx.secondAccountId}}"]'
    And  I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Compte détaché avec succès."

    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.secondAccountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.attachedAccountId}}' with method 'DELETE'
    Then I expect status code is 204
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.ouAccountsId}}' with method 'DELETE'
    Then I expect status code is 204
