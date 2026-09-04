Feature: Test Application details page display

  ################## Application Details ##################
  ## 101 Should display the application details page with its profile panel and audit section
  ## 102 Back button should come back at applications list page
  ## 103 Should display the roles card with an empty table
  ## 104 Add role dialog should close on cancel without creating a role
  ## 105 Add role dialog should display a required validation error on empty name
  ## 106 Should create a role and display it in the table
  ## 107 Created role should be persisted on the backend
  ## 108 Should reject a role with an already used name
  ## 109 Should edit a role and display the updated values
  ## 110 Should delete a role after confirmation
  ## 111 Should display the rules card with no rules
  ## 112 Add rule dialog should close on cancel without creating a rule
  ## 113 Add rule dialog should display required validation errors on empty required fields
  ## 114 Should create a rule and display it in the list with all its sections
  ## 115 Edit rule dialog should close on cancel without modifying the rule
  ## 116 Edit rule dialog should display required validation errors on empty required fields
  ## 117 Should edit a rule and display the updated values in the list
  ## 118 Toggle should show unsaved changes hint and enable the save button
  ## 119 Toggling back should hide the hint and disable the save button
  ## 120 Should save the toggle change and persist it to the backend
  ## 121 Should delete a rule

  ################## Profile panel edition ##################
  ## 201 Edit dialog should open with the current values and close on cancel
  ## 202 Edit dialog should display required validation errors on empty required fields
  ## 203 Should edit the application and display the updated values in the panel

  ################## Export application script ##################
  ## 301 Export button should be enabled

  ################## Test invalid id ####################
  ## 401 Should display an error notification when navigating to a non-existent application
  ## 402 Should display an error notification when navigating to an application with a malformed ID
  Scenario: Roundtrip about Application Details
    ####################################################
    ################## Authentication ##################
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

    ####################################################
    ################## Create application ##############
    ####################################################
    Given I set the viewport size to 1920 px by 1080 px
    And   I visit the '{{ env.E2E_FRONT_URL }}'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/"

    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-detail-101",
        "name": "Application Detail 101",
        "description": "An application for the details page tests",
        "domain": "Security",
        "type": "OIDC",
        "claimsTemplate": "{ \"sub\": \"id\" }"
      }
      """
    Then I expect status code is 201
    And  I store 'applicationId' as '{{response.body.id}}' in context

    When I click on '[data-cy="item_moduleApplicationsPage"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ####################################################
    ################## Application Details #############
    ####################################################

    ## 101 Should display the application details page with its profile panel and audit section
    When I click on '[data-cy="see-button_{{ctx.applicationId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"
    And  I expect the HTML element '[data-cy="generic-details-page"]' to be visible
    And  I expect the HTML element '[data-cy="generic-details-page_title"]' contains "Détails de l'application"
    And  I expect the HTML element '[data-cy="entity-profile-panel"]' to be visible
    And  I expect the HTML element '[data-cy="entity-profile-panel_avatar-img"]' to be visible
    And  I expect the HTML element '[data-cy="entity-profile-panel_status-badge"]' not exists
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "Application Detail 101"
    And  I expect the HTML element '[data-cy="entity-profile-panel_subtitle"]' contains "app-detail-101"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--type"] [data-cy="value"]' contains "OIDC"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--domain"] [data-cy="value"]' contains "Security"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--description"] [data-cy="value"]' contains "An application for the details page tests"
    And  I expect the HTML element '[data-cy="details-section_claims"]' to be visible
    And  I expect the HTML element '[data-cy="details-section_claims"] [data-cy="information-card--claimsTemplate"] [data-cy="value"]' contains '{ "sub": "id" }'
    And  I expect the HTML element '[data-cy="details-section_audit"]' to be visible
    And  I expect the HTML element '[data-cy="details-section_audit"] [data-cy="information-card--createdBy"]' contains "admin_fn admin_ln"
    And  I expect the HTML element '[data-cy="entity-profile-panel_back-button"]' contains "Liste des applications"
    And  I expect the HTML element '[data-cy="entity-profile-panel_edit-button"]' to be visible
    And  I expect the HTML element '[data-cy="button_export"]' to be disabled

    ## 102 Back button should come back at applications list page
    When I click on '[data-cy="entity-profile-panel_back-button"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ####################################################
    ################## Roles management ################
    ####################################################

    ## 103 Should display the roles card with an empty table
    When I click on '[data-cy="see-button_{{ctx.applicationId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"
    And  I expect the HTML element '[data-cy="generic-editable-table-card"]' to be visible
    And  I expect the HTML element '[data-cy="generic-editable-table-card_title"]' contains "Rôles"
    And  I expect the HTML element '[data-cy="generic-editable-table-card_add-button"]' contains "Ajouter un rôle"
    And  I expect the HTML element '[data-cy="generic-entity-table"]' to be visible
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "Aucun rôle pour cette application."

    ## 104 Add role dialog should close on cancel without creating a role
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog_title"]' contains "Ajouter un rôle"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_name"]' contains "Nom"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_description"]' contains "Description"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Ajouter"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "Aucun rôle pour cette application."

    ## 105 Add role dialog should display a required validation error on empty name
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_name"]' contains "Ce champ est requis."
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 106 Should create a role and display it in the table
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "auditor" in the HTML element '[data-cy="field_name"]'
    And  I set the text "Accès en lecture seule" in the HTML element '[data-cy="field_description"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Rôle créé avec succès."
    And  I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "auditor"
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "Accès en lecture seule"

    ## 107 Created role should be persisted on the backend
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/roles' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is "1"
    And  I expect '{{response.body.content[0].name}}' is 'auditor'
    And  I expect '{{response.body.content[0].description}}' is 'Accès en lecture seule'
    And  I store 'roleId' as '{{response.body.content[0].id}}' in context

    ## 108 Should reject a role with an already used name
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "auditor" in the HTML element '[data-cy="field_name"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Impossible de créer le rôle. Veuillez réessayer plus tard."
    And  I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 109 Should edit a role and display the updated values
    When I click on '[data-cy="edit-button_{{ctx.roleId}}"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog_title"]' contains "Modifier le rôle auditor"
    And  I expect the HTML element '[data-cy="field_name"]' to have value "auditor"
    And  I expect the HTML element '[data-cy="field_description"]' to have value "Accès en lecture seule"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Enregistrer"
    When I clear the text in the HTML element '[data-cy="field_name"]'
    And  I set the text "supervisor" in the HTML element '[data-cy="field_name"]'
    And  I clear the text in the HTML element '[data-cy="field_description"]'
    And  I set the text "Supervision des comptes" in the HTML element '[data-cy="field_description"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Rôle modifié avec succès."
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "supervisor"
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "Supervision des comptes"

    ## 110 Should delete a role after confirmation
    When I click on '[data-cy="delete-button_{{ctx.roleId}}"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    And  I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Supprimer le rôle supervisor"
    And  I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Voulez-vous vraiment supprimer le rôle supervisor ?"
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "supervisor"
    When I click on '[data-cy="delete-button_{{ctx.roleId}}"]'
    And  I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Rôle supprimé avec succès."
    And  I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-entity-table"]' contains "Aucun rôle pour cette application."

    ####################################################
    ################## Rules management ################
    ####################################################

    ## 111 Should display the rules card with no rules
    And I expect the HTML element '[data-cy="generic-sortable-list-card"]' to be visible
    And I expect the HTML element '[data-cy="generic-sortable-list-card_title"]' contains "Règles"
    And I expect the HTML element '[data-cy="generic-sortable-list-card_add-button"]' contains "Ajouter une règle"
    And I expect the HTML element '[data-cy="generic-sortable-list-card_save-button"]' contains "Sauvegarder"
    And I expect the HTML element '[data-cy="generic-sortable-list-card_save-button"]' to be disabled
    And I expect the HTML element '[data-cy="generic-sortable-list-card_list"]' contains "Aucune règle pour cette application."

    ## 112 Add rule dialog should close on cancel without creating a rule
    When I click on '[data-cy="generic-sortable-list-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog_title"]' contains "Ajouter une règle"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_code"]' contains "Code"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_description"]' contains "Description"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_priority"]' contains "Priorité"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_script"]' contains "Script"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Ajouter"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    And  I expect the HTML element '[data-cy="field_priority"]' to have value "1"
    And  I expect the HTML element '[data-cy="field_priority"]' to be disabled
    And  I expect the HTML element '[data-cy="field_disabled"]' to have attribute 'aria-checked' with value 'false'
    And  I expect the HTML element '[data-cy="field_disabled"].disabled' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_list"]' contains "Aucune règle pour cette application."

    ## 113 Add rule dialog should display required validation errors on empty required fields
    When I click on '[data-cy="generic-sortable-list-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_code"]' contains "Ce champ est requis."
    When I set the text "test-rule" in the HTML element '[data-cy="field_code"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_script"]' contains "Ce champ est requis."
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 114 Should create a rule and display it in the list with all its sections
    When I click on '[data-cy="generic-sortable-list-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "test-rule" in the HTML element '[data-cy="field_code"]'
    And  I set the text "A test rule" in the HTML element '[data-cy="field_description"]'
    And  I set the text "default allow := false" in the HTML element '[data-cy="field_script"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Règle créée avec succès."
    And  I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"]' to be visible
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"] .drag-handle' to be visible
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-field_code_0"]' contains "test-rule"
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-field_description_0"]' contains "A test rule"
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_button_edit_0"]' to be visible
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_button_delete_0"]' to be visible
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]' to be visible
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]' to have attribute 'aria-checked' with value 'false'

    ## 115 Edit rule dialog should close on cancel without modifying the rule
    When I click on '[data-cy="generic-sortable-list-card_button_edit_0"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog_title"]' contains "Modifier la règle test-rule"
    And  I expect the HTML element '[data-cy="field_code"]' to have value "test-rule"
    And  I expect the HTML element '[data-cy="field_description"]' to have value "A test rule"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="field_priority"]' to be disabled
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="field_disabled"].disabled' to be visible
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Enregistrer"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-field_code_0"]' contains "test-rule"

    ## 116 Edit rule dialog should display required validation errors on empty required fields
    When I click on '[data-cy="generic-sortable-list-card_button_edit_0"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="field_priority"]' to be disabled
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="field_disabled"].disabled' to be visible
    When I clear the text in the HTML element '[data-cy="field_code"]'
    And  I clear the text in the HTML element '[data-cy="field_script"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_code"]' contains "Ce champ est requis."
    When I set the text "test-rule" in the HTML element '[data-cy="field_code"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_script"]' contains "Ce champ est requis."
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 117 Should edit a rule and display the updated values in the list
    When I click on '[data-cy="generic-sortable-list-card_button_edit_0"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="field_priority"]' to be disabled
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="field_disabled"].disabled' to be visible
    When I clear the text in the HTML element '[data-cy="field_code"]'
    And  I set the text "updated-rule" in the HTML element '[data-cy="field_code"]'
    And  I clear the text in the HTML element '[data-cy="field_description"]'
    And  I set the text "An updated rule" in the HTML element '[data-cy="field_description"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-field_code_0"]' contains "updated-rule"
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_item-field_description_0"]' contains "An updated rule"

    ## 118 Toggle should show unsaved changes hint and enable the save button
    When I click on '[data-cy="generic-sortable-list-card_save-button"]'
    Then I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]' to have attribute 'aria-checked' with value 'false'
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_unsaved-changes-hint"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_save-button"]' to be disabled
    When I click on '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]'
    Then I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]' to have attribute 'aria-checked' with value 'true'
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_unsaved-changes-hint"]' to be visible
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_unsaved-changes-hint"]' contains "Des règles ont été modifiées. N'oubliez pas de sauvegarder."
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_save-button"]' to be enabled

    ## 119 Toggling back should hide the hint and disable the save button
    When I click on '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]'
    Then I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]' to have attribute 'aria-checked' with value 'false'
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_unsaved-changes-hint"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_save-button"]' to be disabled

    ## 120 Should save the toggle change and persist it to the backend
    When I click on '[data-cy="generic-sortable-list-card_item-0"] [data-cy="field_disabled"]'
    And  I click on '[data-cy="generic-sortable-list-card_save-button"]'
    Then I expect the HTML element '[data-cy="generic-sortable-list-card_unsaved-changes-hint"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Modifications sauvegardées avec succès."
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content[0].disabled}}' is "false"

    ## 121 Should delete a rule
    When I click on '[data-cy="generic-sortable-list-card_button_delete_0"]'
    Then I expect the HTML element '[data-cy="generic-sortable-list-card_item-0"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_save-button"]' to be enabled
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_unsaved-changes-hint"]' contains "Des règles ont été modifiées. N'oubliez pas de sauvegarder."
    When I click on '[data-cy="generic-sortable-list-card_save-button"]'
    Then I expect the HTML element '[data-cy="generic-sortable-list-card_unsaved-changes-hint"]' not exists
    And  I expect the HTML element '[data-cy="generic-sortable-list-card_save-button"]' to be disabled
    And  I expect the HTML element '.q-notification__message' contains "Modifications sauvegardées avec succès."
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/rules' with method 'GET'
    Then I expect status code is 200
    And  I expect "{{ response.body.content }}" to have length 0

    ###########################################################
    ################## Profile panel edition ##################
    ###########################################################

    ## 201 Edit dialog should open with the current values and close on cancel
    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog"]' contains "Modifier Application Detail 101"
    And  I expect the HTML element '[data-cy="field_code"]' to have value "app-detail-101"
    And  I expect the HTML element '[data-cy="field_name"]' to have value "Application Detail 101"
    And  I expect the HTML element '[data-cy="field_type"]' to have value "OIDC"
    And  I expect the HTML element '[data-cy="field_domain"]' to have value "Security"
    And  I expect the HTML element '[data-cy="field_description"]' to have value "An application for the details page tests"
    And  I expect the HTML element '[data-cy="field_claimsTemplate"]' to have value '{ "sub": "id" }'
    And  I expect the HTML element '[data-cy="form-dialog_field-container_domain"]' contains "Domaine applicatif"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_claimsTemplate"]' contains "Modèle de génération des claims"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Enregistrer"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "Application Detail 101"

    ## 202 Edit dialog should display required validation errors on empty required fields
    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I clear the text in the HTML element '[data-cy="field_code"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_code"]' contains "Ce champ est requis."
    When I clear the text in the HTML element '[data-cy="field_name"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_name"]' contains "Ce champ est requis."
    When I clear the text in the HTML element '[data-cy="field_type"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_type"]' contains "Ce champ est requis."
    When I clear the text in the HTML element '[data-cy="field_claimsTemplate"]'
    Then I expect the HTML element '[data-cy="form-dialog_field-container_claimsTemplate"]' contains "Ce champ est requis."
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="entity-profile-panel_subtitle"]' contains "app-detail-101"

    ## 203 Should edit the application and display the updated values in the panel
    When I click on '[data-cy="entity-profile-panel_edit-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I clear the text in the HTML element '[data-cy="field_name"]'
    And  I set the text "Application Detail 101 edited" in the HTML element '[data-cy="field_name"]'
    And  I clear the text in the HTML element '[data-cy="field_domain"]'
    And  I set the text "Identity" in the HTML element '[data-cy="field_domain"]'
    And  I clear the text in the HTML element '[data-cy="field_description"]'
    And  I set the text "An edited application" in the HTML element '[data-cy="field_description"]'
    And  I clear the text in the HTML element '[data-cy="field_claimsTemplate"]'
    And  I set the text '{% raw %}{{} "sub": "login" }{% endraw %}' in the HTML element '[data-cy="field_claimsTemplate"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Application modifiée avec succès."
    And  I expect the HTML element '[data-cy="entity-profile-panel_title"]' contains "Application Detail 101 edited"
    And  I expect the HTML element '[data-cy="entity-profile-panel_subtitle"]' contains "app-detail-101"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--domain"] [data-cy="value"]' contains "Identity"
    And  I expect the HTML element '[data-cy="entity-profile-panel"] [data-cy="information-card--description"] [data-cy="value"]' contains "An edited application"
    And  I expect the HTML element '[data-cy="details-section_claims"] [data-cy="information-card--claimsTemplate"] [data-cy="value"]' contains '{ "sub": "login" }'

    ###########################################################
    ################## Export application script ##############
    ###########################################################
    ## 301 Export button should be enabled
    When I click on '[data-cy="entity-profile-panel_back-button"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    When I click on '[data-cy="see-button_{{ctx.applicationId}}"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"
    And  I expect the HTML element '[data-cy="button_export"]' to be enabled

    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then I expect status code is 204

  ####################################################
  ################## Test invalid id #################
  ####################################################
  Scenario: 401 - Should display an error notification when navigating to a non-existent application
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

    Given I set the viewport size to 1920 px by 1080 px
    And   I visit the '{{ env.E2E_FRONT_URL }}'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/"

    When I visit the "{{ env.E2E_FRONT_URL }}/applications/00000000-0000-4000-8000-000000000000"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And  I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

  Scenario: 402 - Should display an error notification when navigating to an application with a malformed ID
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

    Given I set the viewport size to 1920 px by 1080 px
    And   I visit the '{{ env.E2E_FRONT_URL }}'
    When  I set the text "admin" in the HTML element "input#userfield"
    And   I set the text "password" in the HTML element "input#passwordfield"
    And   I click on "button.btn-success"
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/"

    When I visit the "{{ env.E2E_FRONT_URL }}/applications/not-a-valid-uuid"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And  I expect current url is "{{ env.E2E_FRONT_URL }}/applications"
