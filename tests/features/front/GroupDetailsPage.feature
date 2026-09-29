Feature: Test Group details page

  ################## Group details ##################
  ## 101 Should display the group details page with the profile panel and the audit section
  ## 102 Back button should come back at groups list page

  ################## Profile panel edition ##################
  ## 201 Edit dialog should open with the current values and close on cancel
  ## 202 Edit dialog should display required validation errors on empty required fields
  ## 203 Should edit the group and display the updated values in the panel
  ## 204 Edit dialog should exclude the edited group from the parent group options

  ################## Accounts of the group ##################
  ## 301 Should display an empty accounts card for a group without account
  ## 302 Attach dialog should close on cancel without attaching an account
  ## 303 Should attach an existing account from the dialog
  ## 304 Should detach an account after confirmation

  Scenario: Roundtrip about Group details

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
    ################## Create group ####################
    ####################################################
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
    And  I store 'rootId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I store 'linidId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/groups' with method 'POST' with body:
      """
      {
        "code": "e2e-group-details",
        "name": "E2E Group Details",
        "parentId": "00000000-0000-4000-8000-000000009002",
        "description": "A group dedicated to the details page tests",
        "email": "e2e-group-details@example.com",
        "organizationalUnitId": "{{ctx.rootId}}",
        "applicationId": "{{ctx.linidId}}"
      }
      """
    Then I expect status code is 201
    And  I store 'groupId' as '{{response.body.id}}' in context

    ####################################################
    ################## Group details ###################
    ####################################################

    ## 101 Should display the group details page with the profile panel and the audit section
    When I click on '[data-cy="item_moduleGroupsPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups"
    When I click on '[data-cy="see-button_{{ctx.groupId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups/{{ctx.groupId}}"
    And  I expect the HTML element '[data-cy="generic-details-page"]' to be visible
    And  I expect the HTML element '[data-cy="entity-profile-panel"]' to be visible
    And  I expect the HTML element '[data-cy="entity-profile-panel_avatar-img"]' to be visible
    And  I expect the HTML element '[data-cy="entity-profile-panel_status-badge"]' not exists
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "E2E Group Details"
    And  I expect the HTML element '[data-cy="entity-profile-panel_subtitle"]' contains "e2e-group-details"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--parentName"] [data-cy="title"]' contains "Groupe parent"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--parentName"] [data-cy="value"]' contains "IT Department"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--email"] [data-cy="value"]' contains "e2e-group-details@example.com"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--description"] [data-cy="value"]' contains "A group dedicated to the details page tests"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--organizationalUnitName"] [data-cy="title"]' contains "Unité organisationnelle"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--organizationalUnitName"] [data-cy="value"]' contains "root"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--applicationName"] [data-cy="value"]' contains "LINID - Identity Manager"
    And  I expect the HTML element '[data-cy="details-section_audit"]' to be visible
    And  I expect the HTML element '[data-cy="details-section_audit"] [data-cy="information-card--createdBy"] [data-cy="value"]' contains "admin_fn admin_ln"
    And  I expect the HTML element '[data-cy="details-section_audit"] [data-cy="information-card--insertDate"]' to be visible
    And  I expect the HTML element '[data-cy="entity-profile-panel_back-button"]' contains "Liste des groupes"
    And  I expect the HTML element '[data-cy="entity-profile-panel_edit-button"]' to be visible

    ## 102 Back button should come back at groups list page
    When I click on '[data-cy="entity-profile-panel_back-button"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups"

    ####################################################
    ################## Profile panel edition ###########
    ####################################################

    ## 201 Edit dialog should open with the current values and close on cancel
    When I click on '[data-cy="see-button_{{ctx.groupId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/groups/{{ctx.groupId}}"
    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog"]' contains "Modifier E2E Group Details"
    And  I expect the HTML element '[data-cy="field_code"]' to have value "e2e-group-details"
    And  I expect the HTML element '[data-cy="field_name"]' to have value "E2E Group Details"
    And  I expect the HTML element '[data-cy="field_description"]' to have value "A group dedicated to the details page tests"
    And  I expect the HTML element '[data-cy="field_email"]' to have value "e2e-group-details@example.com"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_code"]' contains "Code *"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_name"]' contains "Libellé *"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_parentId"]' contains "Groupe parent"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_parentId"]' not contains "Groupe parent *"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_organizationalUnitId"]' not contains "Unité organisationnelle *"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_applicationId"]' not contains "Application *"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_description"]' not contains "Description *"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_email"]' not contains "Email *"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Enregistrer"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "E2E Group Details"

    ## 202 Edit dialog should display required validation errors on empty required fields
    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I clear the text in the HTML element '[data-cy="field_code"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_code"]' contains "Ce champ est requis."
    When I clear the text in the HTML element '[data-cy="field_name"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_name"]' contains "Ce champ est requis."
    When I set the text "invalid code!" in the HTML element '[data-cy="field_code"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_code"]' contains "Seuls les lettres, les chiffres, les tirets et les tirets bas sont autorisés."
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="entity-profile-panel_subtitle"]' contains "e2e-group-details"

    ## 203 Should edit the group and display the updated values in the panel
    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I clear the text in the HTML element '[data-cy="field_name"]'
    And  I set the text "E2E Group Details edited" in the HTML element '[data-cy="field_name"]'
    And  I clear the text in the HTML element '[data-cy="field_description"]'
    And  I set the text "An edited group" in the HTML element '[data-cy="field_description"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Groupe modifié avec succès."
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "E2E Group Details edited"
    And  I expect the HTML element '[data-cy="entity-profile-panel_subtitle"]' contains "e2e-group-details"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--description"] [data-cy="value"]' contains "An edited group"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--parentName"] [data-cy="value"]' contains "IT Department"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--organizationalUnitName"] [data-cy="value"]' contains "root"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--applicationName"] [data-cy="value"]' contains "LINID - Identity Manager"

    ## 204 Edit dialog should exclude the edited group from the parent group options
    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I click on '[data-cy="field_parentId"] .q-select__focus-target'
    Then I expect the HTML element '[role="listbox"]' to be visible
    And  I expect the HTML element '[role="listbox"]' contains "All Staff"
    And  I expect the HTML element '[role="listbox"]' contains "IT Department"
    And  I expect the HTML element '[role="listbox"]' contains "Developers"
    And  I expect the HTML element '[role="listbox"]' not contains "E2E Group Details"
    When I click on '[data-cy="form-dialog_title"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ####################################################
    ################## Accounts of the group ###########
    ####################################################

    ## 301 Should display an empty accounts card for a group without account
    And  I expect the HTML element '[data-cy="generic-editable-table-card"]' to be visible
    And  I expect the HTML element '[data-cy="generic-editable-table-card_title"]' contains "Comptes"
    And  I expect the HTML element '[data-cy="generic-editable-table-card_add-button"]' contains "Attacher un compte"
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Aucun compte rattaché à ce groupe."

    ## 302 Attach dialog should close on cancel without attaching an account
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog_title"]' contains "Attacher un compte"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_accountId"]' contains "Compte"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Attacher"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Aucun compte rattaché à ce groupe."

    ## 303 Should attach an existing account from the dialog
    When I request '{{env.E2E_API_URL}}/accounts' with method 'POST' with body:
      """
      {
        "externalId": "ext-gra-ui-1",
        "lastname": "Lefevre",
        "firstname": "Chloe",
        "email": "chloe-gra@example.com",
        "validityPeriod": {
          "start": "2080-01-01T00:00:00Z",
          "end": "2100-01-01T00:00:00Z"
        },
        "organizationalUnit": "{{ctx.rootId}}",
        "roleId": "00000000-0000-4000-8000-00000000f001",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201
    And  I store 'attachedAccountId' as '{{response.body.id}}' in context

    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    # The account list is virtualized: the seeded accounts all fit in the first page of 50, so a
    # single scroll renders the last options of the menu, the newly created account among them.
    When I click on '[data-cy="field_accountId"]'
    And  I scroll to 'bottom' into '.q-menu'
    And  I click on '.q-menu .q-item:contains("Lefevre Chloe")'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Compte attaché avec succès."
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Lefevre"
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Chloe"
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "chloe-gra@example.com"
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' not contains "Aucun compte rattaché à ce groupe."
    # The relationship carries no functional role: the card offers no edit action
    And  I expect the HTML element '[data-cy="edit-button_{{ctx.attachedAccountId}}"]' not exists
    And  I expect the HTML element '[data-cy="delete-button_{{ctx.attachedAccountId}}"]' contains "Détacher"

    ## 304 Should detach an account after confirmation
    When I click on '[data-cy="delete-button_{{ctx.attachedAccountId}}"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    And  I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Détacher le compte Lefevre Chloe"
    And  I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Voulez-vous vraiment détacher le compte Lefevre Chloe"
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Lefevre"
    When I click on '[data-cy="delete-button_{{ctx.attachedAccountId}}"]'
    And  I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Compte détaché avec succès."
    And  I expect the HTML element '[data-cy="generic-editable-table-card"] [data-cy="generic-entity-table"]' contains "Aucun compte rattaché à ce groupe."

    ## Cleanup
    When I request '{{env.E2E_API_URL}}/accounts/{{ctx.attachedAccountId}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/groups/{{ctx.groupId}}' with method 'DELETE'
    Then I expect status code is 204
