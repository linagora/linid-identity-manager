Feature: Test Role homepage display

  ################## Role Homepage ##################
  ## 101 Should display the role homepage with all roles listed
  ## 102 Should filter roles when using advanced search
  ## 103 Should open the edit dialog with the current description and close on cancel
  ## 104 Should edit the description and display the updated value in the table
  ## 105 Should restore the original description
  ## 106 Should filter roles by organizational unit
  ## 107 Should disable the deletion of a role held by an account
  ## 108 Should delete a role after confirmation

  Scenario: Roundtrip about Role homepage

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
    ################## Role Homepage ##################
    ####################################################

    ## 101 Should display the role homepage with all roles listed
    Given I store "roleId" as "00000000-0000-4000-8000-00000000b006" in context
    When  I click on '[data-cy="item_moduleRolesPage"]'
    Then  I expect current url is "{{ env.E2E_FRONT_URL }}/roles"
    And   I expect the HTML element '.linid-smart-filter' to be visible
    And   I expect the HTML element '.generic-entity-table' to be visible
    And   I expect the HTML element '[data-cy="item-row"]' appear 7 times on screen
    And   I expect the HTML element '[data-cy="cell-code_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-code_{{ctx.roleId}}"]' contains "AUDITOR"
    And   I expect the HTML element '[data-cy="cell-name_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-name_{{ctx.roleId}}"]' contains "Auditor"
    And   I expect the HTML element '[data-cy="cell-description_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-description_{{ctx.roleId}}"]' contains "Reviews compliance and security controls."
    And   I expect the HTML element '[data-cy="cell-organizationalUnits_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-organizationalUnits_00000000-0000-4000-8000-00000000b002"]' contains "Company A, Company B"
    And   I expect the HTML element '[data-cy="cell-createdBy_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-createdBy_{{ctx.roleId}}"]' contains "admin_fn admin_ln"
    And   I expect the HTML element '[data-cy="cell-insertDate_{{ctx.roleId}}"]' to be visible
    And   I expect the HTML element '[data-cy="cell-table_actions_{{ctx.roleId}}"]' to be visible

    ## 102 Should filter roles when using advanced search
    When I click on ".linid-smart-filter"
    And  I set the text "unknown" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles?code=lk_*unknown*"
    And  I expect the HTML element '[data-cy="item-row"]' appear 0 times on screen

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 7 times on screen

    When I click on ".linid-smart-filter"
    And  I set the text "AUDITOR" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles?code=lk_*AUDITOR*"
    And  I expect the HTML element '[data-cy="item-row"]' to be visible
    And  I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And  I expect the HTML element '[data-cy="cell-code_{{ctx.roleId}}"]' to be visible

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 7 times on screen

    ## 103 Should open the edit dialog with the current description and close on cancel
    When I click on '[data-cy="cell-table_actions_{{ctx.roleId}}"] [data-cy="form-dialog-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    And  I expect the HTML element '[data-cy="form-dialog_title"]' contains "Modifier le rôle Auditor"
    And  I expect the HTML element '[data-cy="form-dialog_field-container_description"]' contains "Description"
    And  I expect the HTML element '[data-cy="field_description"]' to have value "Reviews compliance and security controls."
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_confirm"]' contains "Enregistrer"
    And  I expect the HTML element '[data-cy="form-dialog"] [data-cy="button_cancel"]' contains "Annuler"
    When I click on '[data-cy="form-dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '[data-cy="cell-description_{{ctx.roleId}}"]' contains "Reviews compliance and security controls."

    ## 104 Should edit the description and display the updated value in the table
    When I click on '[data-cy="cell-table_actions_{{ctx.roleId}}"] [data-cy="form-dialog-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I clear the text in the HTML element '[data-cy="field_description"]'
    And  I set the text "Reviews compliance and security controls. Updated by E2E." in the HTML element '[data-cy="field_description"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Le rôle a été mis à jour avec succès."
    And  I expect the HTML element '[data-cy="cell-description_{{ctx.roleId}}"]' contains "Reviews compliance and security controls. Updated by E2E."

    ## 105 Should restore the original description
    When I click on '[data-cy="cell-table_actions_{{ctx.roleId}}"] [data-cy="form-dialog-button"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' to be visible
    When I clear the text in the HTML element '[data-cy="field_description"]'
    And  I set the text "Reviews compliance and security controls." in the HTML element '[data-cy="field_description"]'
    And  I click on '[data-cy="form-dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="form-dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Le rôle a été mis à jour avec succès."
    And  I expect the HTML element '[data-cy="cell-description_{{ctx.roleId}}"]' contains "Reviews compliance and security controls."

    ## 106 Should filter roles by organizational unit
    When I click on ".linid-smart-filter"
    And  I click on '[data-cy="linid-filter-panel_item-organizationalUnitId"]'
    And  I click on '[data-cy="generic-tree-checkbox-00000000-0000-4000-8000-00000000000a"]'
    And  I click on '[data-cy="tree-search-filter-panel_search"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/roles?organizationalUnitId=00000000-0000-4000-8000-00000000000a"
    And  I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    And  I expect the HTML element '[data-cy="cell-code_00000000-0000-4000-8000-00000000b002"]' contains "MANAGER"

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 7 times on screen

    ## 107 Should disable the deletion of a role held by an account
    Then I expect the HTML element '[data-cy="cell-table_actions_00000000-0000-4000-8000-00000000b001"] [data-cy="confirm-dialog-button"]' to be disabled
    And  I expect the HTML element '[data-cy="cell-table_actions_{{ctx.roleId}}"] [data-cy="confirm-dialog-button"]' to be enabled

    ## 108 Should delete a role after confirmation
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

    When I request '{{env.E2E_API_URL}}/roles' with method 'POST' with body:
      """
      {
        "code": "E2E-ROLE-DELETE",
        "name": "E2E Role Delete",
        "description": "Role created by the roles page scenarios",
        "extraParameters": {}
      }
      """
    Then I expect status code is 201

    When I click on ".linid-smart-filter"
    And  I click on '[data-cy="linid-filter-panel_item-code"]'
    And  I set the text "E2E-ROLE-DELETE" in the HTML element '[data-cy="text-search-filter-panel_input"]'
    And  I click on '[data-cy="text-search-filter-panel_search"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="item-row"] [data-cy="confirm-dialog-button"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' to be visible
    And  I expect the HTML element '[data-cy="confirmation_dialog_title"]' contains "Supprimer le rôle E2E Role Delete"
    And  I expect the HTML element '[data-cy="confirmation_dialog_content"]' contains "Voulez-vous vraiment supprimer le rôle E2E Role Delete ?"
    When I click on '[data-cy="confirmation_dialog"] [data-cy="button_cancel"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '[data-cy="item-row"]' appear 1 times on screen
    When I click on '[data-cy="item-row"] [data-cy="confirm-dialog-button"]'
    And  I click on '[data-cy="confirmation_dialog"] [data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="confirmation_dialog"]' not exists
    And  I expect the HTML element '.q-notification__message' contains "Le rôle a été supprimé avec succès."
    And  I expect the HTML element '[data-cy="item-row"]' appear 0 times on screen

    When I click on '[data-cy="linid-smart-filter-field"] [aria-label="Remove"]'
    Then I expect the HTML element '[data-cy="item-row"]' appear 7 times on screen
