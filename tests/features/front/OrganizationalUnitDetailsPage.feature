Feature: Test Organizational Unit details panel display

  ## 101 Should reach the details page from the table and display title, badge and OU information for an active OU
  ## 102 Should display the suspension dropdown for an active OU
  ## 103 Immediate suspension should suspend the OU once confirmed
  ## 104 Schedule suspension should open the form dialog with date and reason fields
  ## 105 Schedule suspension should succeed when localized dates are submitted
  ## 106 Should display the suspended banner and badge for a currently suspended OU
  ## 107 Immediate reactivation from the banner should schedule the reactivation once confirmed
  ## 108 Edit suspension end from the banner should open the form dialog
  ## 109 Edit suspension end should succeed when a localized date is submitted
  ## 110 Visiting an unknown organizational unit should display a not found notification
  ## 111 Create child button should open the creation page with the current OU as parent

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
    And I expect the HTML element '[data-cy="organizational-unit-details-page"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-details-page_title"]' contains "Détails de l'unité organisationnelle"
    And I expect the HTML element '[data-cy="status-badge_active"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-details-page_cards"]' to be visible
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "Company A"

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
    And I expect the HTML element '[data-cy="status-badge_active"]' to be visible
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
    And I expect the HTML element '[data-cy="information-card--name"] [data-cy="value"]' contains "SuspendedOuNoEnd"
    And I expect the HTML element '[data-cy="status-badge_suspended"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-suspended-banner"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-activation-actions"]' not exists
    And I expect the HTML element '[data-cy="organizational-unit-suspension-actions"]' not exists

    ## 107 Immediate reactivation from the banner should schedule the reactivation once confirmed
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
    And I expect the HTML element ".q-notification__message" contains "L'unité organisationnelle sera réactivée dans une heure."

    ## 108 Edit suspension end from the banner should open the form dialog
    When I click on '[data-cy="organizational-unit-suspended-banner_modify-suspension-end-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Modifier la fin de suspension"
    And I expect the HTML element '[data-cy="form-dialog_field-container_end"]' to be visible
    And I expect the HTML element '[data-cy="field_end"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 109 Edit suspension end should succeed when a localized date is submitted
    When I click on '[data-cy="organizational-unit-suspended-banner_modify-suspension-end-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "31/12/2100" in the HTML element '[data-cy="field_end"]'
    And I select '.q-menu .q-item:contains("Suspension Reason A")' in '[data-cy="field_reason"]'
    And I select '.q-menu .q-item:contains("Suspension Sub-reason A.1")' in '[data-cy="field_subreason"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Date de fin de suspension mise à jour avec succès."

    ## 110 Visiting an unknown organizational unit should display a not found notification
    Given I visit the "{{ env.E2E_FRONT_URL }}/organizational-units/00000000-0000-4000-8000-deadbeefdead"
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/00000000-0000-4000-8000-deadbeefdead"
    And I expect the HTML element '[data-cy="organizational-unit-details-page"]' to be visible
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Unité organisationnelle introuvable."

    ## 111 Create child button should open the creation page with the current OU as parent
    When I click on '[data-cy="item_moduleOrganizationalUnitsPage"]'
    Then I expect current url contains "{{ env.E2E_FRONT_URL }}/organizational-units"
    When I click on '[data-cy="linid-smart-filter-field"]'
    And I set the text "Company A" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="see-button_{{ctx.company_a_id}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.company_a_id}}"
    And I expect the HTML element '[data-cy="organizational-unit-details-page_create-child-button"]' to be visible
    When I click on '[data-cy="organizational-unit-details-page_create-child-button"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/organizational-units/create?parent={{ctx.company_a_id}}"
    And I expect the HTML element '[data-cy="organizational-unit-creation-page"]' to be visible
    And I expect the HTML element '[data-cy="field_parent"] input' to have value "Company A"
