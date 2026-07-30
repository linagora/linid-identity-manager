Feature: Test Application details page display

  ################## Application Details ##################
  ## 101 Should display the application details page with its sections
  ## 102 Back button should come back at applications list page
  ## 103 Should display the roles card with an empty table
  ## 104 Add role dialog should close on cancel without creating a role
  ## 105 Add role dialog should display a required validation error on empty name
  ## 106 Should create a role and display it in the table
  ## 107 Created role should be persisted on the backend
  ## 108 Should reject a role with an already used name
  ## 109 Should edit a role and display the updated values
  ## 110 Should delete a role after confirmation
  ## 111 Should display an error notification when navigating to a non-existent application
  ## 112 Should display an error notification when navigating to an application with a malformed ID
  ## 113 Remove the application

  Scenario: Roundtrip about Application Details

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
    ################## Create application ##############
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
    When I request '{{env.E2E_API_URL}}/applications' with method 'POST' with body:
      """
      {
        "code": "app-detail-101",
        "name": "Application Detail 101",
        "description": "An application for the details page tests",
        "type": "OIDC",
        "claimsTemplate": "{ \"sub\": \"id\" }"
      }
      """
    Then I expect status code is 201
    And I store 'applicationId' as '{{response.body.id}}' in context

    ####################################################
    ################## Application Details #############
    ####################################################

    ## 101 Should display the application details page with its sections
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"
    Then I expect the HTML element '[data-cy="generic-details-page"]' to be visible
    And I expect the HTML element '[data-cy="generic-details-page_title"]' contains "Détails de l'application"
    And I expect the HTML element '[data-cy="details-section_identity"]' to be visible
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--code"]' contains "app-detail-101"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--name"]' contains "Application Detail 101"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--description"]' contains "An application for the details page tests"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--type"]' contains "OIDC"
    And I expect the HTML element '[data-cy="details-section_audit"]' to be visible
    And I expect the HTML element '[data-cy="details-section_audit"] [data-cy="information-card--createdBy"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_cancel"]' contains "Retour"

    ## 102 Back button should come back at applications list page
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ####################################################
    ################## Roles management ################
    ####################################################

    ## 103 Should display the roles card with an empty table
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"
    Then I expect the HTML element '[data-cy="generic-editable-table-card"]' to be visible
    And I expect the HTML element '[data-cy="generic-editable-table-card_title"]' contains "Rôles"
    And I expect the HTML element '[data-cy="generic-editable-table-card_add-button"]' contains "Ajouter un rôle"
    And I expect the HTML element '[data-cy="generic-entity-table"]' to be visible
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "Aucun rôle pour cette application."

    ## 104 Add role dialog should close on cancel without creating a role
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Ajouter un rôle"
    And I expect the HTML element '[data-cy="form-dialog_field-container_name"]' contains "Nom"
    And I expect the HTML element '[data-cy="form-dialog_field-container_description"]' contains "Description"
    And I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Ajouter"
    And I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "Aucun rôle pour cette application."

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
    And I set the text "Accès en lecture seule" in the HTML element '[data-cy="field_description"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Rôle créé avec succès."
    And I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "auditor"
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "Accès en lecture seule"

    ## 107 Created role should be persisted on the backend
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}/roles' with method 'GET'
    Then I expect status code is 200
    And I expect '{{response.body.totalElements}}' is "1"
    And I expect '{{response.body.content[0].name}}' is 'auditor'
    And I expect '{{response.body.content[0].description}}' is 'Accès en lecture seule'
    And I store 'roleId' as '{{response.body.content[0].id}}' in context

    ## 108 Should reject a role with an already used name
    When I click on '[data-cy="generic-editable-table-card_add-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I set the text "auditor" in the HTML element '[data-cy="field_name"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Impossible de créer le rôle. Veuillez réessayer plus tard."
    And I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists

    ## 109 Should edit a role and display the updated values
    When I click on '[data-cy="edit-button_{{ctx.roleId}}"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And I expect the HTML element '[data-cy="form-dialog_title"]' contains "Modifier le rôle auditor"
    And I expect the HTML element '[data-cy="field_name"]' to have value "auditor"
    And I expect the HTML element '[data-cy="field_description"]' to have value "Accès en lecture seule"
    And I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Enregistrer"
    When I clear the text in the HTML element '[data-cy="field_name"]'
    And I set the text "supervisor" in the HTML element '[data-cy="field_name"]'
    And I clear the text in the HTML element '[data-cy="field_description"]'
    And I set the text "Supervision des comptes" in the HTML element '[data-cy="field_description"]'
    And I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Rôle modifié avec succès."
    And I expect the HTML element '[data-cy="form-dialog"]' not exists
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "supervisor"
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "Supervision des comptes"

    ## 110 Should delete a role after confirmation
    When I click on '[data-cy="delete-button_{{ctx.roleId}}"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    And I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Supprimer le rôle supervisor"
    And I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Voulez-vous vraiment supprimer le rôle supervisor ?"
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "supervisor"
    When I click on '[data-cy="delete-button_{{ctx.roleId}}"]'
    And I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Rôle supprimé avec succès."
    And I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And I expect the HTML element '[data-cy="generic-entity-table"]' contains "Aucun rôle pour cette application."

    ####################################################
    ################## Cleanup and errors ##############
    ####################################################

    ## 111 Should display an error notification when navigating to a non-existent application
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/00000000-0000-4000-8000-000000000000"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ## 112 Should display an error notification when navigating to an application with a malformed ID
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/not-a-valid-uuid"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ## 113 Remove the application
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then I expect status code is 204
