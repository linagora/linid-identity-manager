Feature: Test Organizational Unit details page display

  ## 101 Should display the page title, badge and OU information
  ## 102 Should display the suspension dropdown for an active OU
  ## 103 Should display a not found notification when navigating to a non-existent OU
  ## 104 Schedule suspension should succeed when localized dates are submitted
  ## 105 Immediate suspension should suspend the OU once confirmed
  ## 106 Schedule suspension should open the form dialog with date fields
  ## 107 Cleanup created OUs
  # The following scenarios cannot be played end-to-end and are left as TODO:
  # ## 1xx Currently suspended state with banner + reactivation actions
  # ##     (backend rejects a suspension start in the past, so a currently
  # ##      suspended OU cannot be seeded via the public API)
  # ## 1xx Immediate reactivation from the banner (same backend constraint)
  # ## 1xx Edit suspension end from the banner (same backend constraint)
  # ## 1xx Navigate to the OU details page from an OU listing
  # ##     (no OU listing page exists yet)

  Scenario: Roundtrip about Organizational Unit details

    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}'
    When I set the text "admin" in the HTML element "input#userfield"
    And I set the text "password" in the HTML element "input#passwordfield"
    And I click on "button.btn-success"
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/"

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

    When I request '{{env.E2E_API_URL}}/organizational-units' with method 'POST' with body:
      """
      {"parent": "{{ctx.rootID}}", "name": "ActiveOUE2E", "type": "COMPANY"}
      """
    Then I expect status code is 201
    And I store 'activeOuId' as '{{response.body.id}}' in context

    # TODO: navigate from the organizational units listing page once it exists,
    # instead of visiting the details URL directly. Applies to every "I visit"
    # of an organizational unit in this scenario.
    Given I visit the "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.activeOuId}}"

    ## 101 Should display the page title, badge and OU information
    Then I expect the HTML element '[data-cy="organizational-unit-details-page"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-details-page_title"]' contains "Détails de l'unité organisationnelle"
    And I expect the HTML element '[data-cy="status-badge_active"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-details-page_cards"]' to be visible

    ## 102 Should display the suspension dropdown for an active OU
    And I expect the HTML element '[data-cy="organizational-unit-suspension-actions"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-suspended-banner"]' not exists

    ## 103 Should display a not found notification when navigating to a non-existent OU
    Given I visit the "{{ env.E2E_FRONT_URL }}/organizational-units/00000000-0000-4000-8000-deadbeefdead"
    Then I expect the HTML element ".q-notification__message" contains "Unité organisationnelle introuvable."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/"

    ## 104 Schedule suspension should succeed when localized dates are submitted
    Given I visit the "{{ env.E2E_FRONT_URL }}/organizational-units/{{ctx.activeOuId}}"
    When I click on '[data-cy="organizational-unit-suspension-actions"]'
    And I click on '[data-cy="dropdown-button_item_suspension.scheduled"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "01/01/2099" in the HTML element '[data-cy="field_start"]'
    And I set the text "31/12/2099" in the HTML element '[data-cy="field_end"]'
    And I select '.q-menu .q-item:contains("Suspension Reason A")' in '[data-cy="field_reason"]'
    And I select '.q-menu .q-item:contains("Suspension Sub-reason A.1")' in '[data-cy="field_subreason"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Suspension planifiée avec succès."
    And I expect the HTML element '[data-cy="status-badge_active"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-suspended-info-text"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-suspension-actions"]' to be visible
    And I expect the HTML element '[data-cy="organizational-unit-suspended-banner"]' not exists

    ## 105 Immediate suspension should suspend the OU once confirmed
    When I click on '[data-cy="organizational-unit-suspension-actions"]'
    And I click on '[data-cy="dropdown-button_item_suspension.immediate"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    And I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Suspendre l'unité organisationnelle"
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    When I click on '[data-cy="organizational-unit-suspension-actions"]'
    And I click on '[data-cy="dropdown-button_item_suspension.immediate"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Unité organisationnelle suspendue avec succès."

    ## 106 Schedule suspension should open the form dialog with date fields
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

    ## 107 Cleanup created OUs
    When I request '{{env.E2E_API_URL}}/organizational-units/{{ctx.activeOuId}}' with method 'DELETE'
    Then I expect status code is 204
