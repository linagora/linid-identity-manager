Feature: Test Application edition page display

  ################## Application Edition ##################
  ## 101 Should display the application details page with its sections
  ## 102 Should display the application edition page with its sections
  ## 103 Should come back to details page when clicking on the cancel button
  ## 104 Submitting an empty form should display required validation messages
  ## 105 Should modify the application with the edition page
  ## 106 Should display an error notification when navigating to a non-existent application
  ## 107 Should display an error notification when navigating to an application with a malformed ID
  ## 108 Remove the application

  Scenario: Roundtrip about Application Edition

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
        "code": "app-edition-101",
        "name": "Application Edition 101",
        "description": "An application for the edition page tests",
        "domain": "Security",
        "type": "OIDC",
        "claimsTemplate": "{ \"sub\": \"id\" }"
      }
      """
    Then I expect status code is 201
    And I store 'applicationId' as '{{response.body.id}}' in context

    ## 101 Should display the application details page with its sections
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"
    Then I expect the HTML element '[data-cy="generic-details-page"]' to be visible
    And I expect the HTML element '[data-cy="generic-details-page_title"]' contains "Détails de l'application"
    And I expect the HTML element '[data-cy="details-section_identity"]' to be visible
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--code"]' contains "app-edition-101"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--name"]' contains "Application Edition 101"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--description"]' contains "An application for the edition page tests"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--domain"]' contains "Security"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--type"]' contains "OIDC"
    And I expect the HTML element '[data-cy="details-section_audit"]' to be visible
    And I expect the HTML element '[data-cy="details-section_audit"] [data-cy="information-card--createdBy"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_cancel"]' contains "Retour"

    ####################################################
    ################## Application edition #############
    ####################################################

    ## 102 Should display the application edition page with its sections
    When I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}/edit"
    And I expect the HTML element '[data-cy="generic-edition-page"]' to be visible
    And I expect the HTML element '[data-cy="generic-edition-page_title"]' contains "Modifier l'application"
    And I expect the HTML element '[data-cy="field-container_general"]' to be visible
    And I expect the HTML element '[data-cy="field_code"]' to be visible
    And I expect the HTML element '[data-cy="field_code"]' to have value "app-edition-101"
    And I expect the HTML element '[data-cy="field_name"]' to be visible
    And I expect the HTML element '[data-cy="field_name"]' to have value "Application Edition 101"
    And I expect the HTML element '[data-cy="field_type"]' to be visible
    And I expect the HTML element '[data-cy="field_type"]' to have value "OIDC"
    And I expect the HTML element '[data-cy="field_description"]' to be visible
    And I expect the HTML element '[data-cy="field_description"]' to have value "An application for the edition page tests"
    And I expect the HTML element '[data-cy="field_domain"]' to be visible
    And I expect the HTML element '[data-cy="field_domain"]' to have value "Security"
    And I expect the HTML element '[data-cy="field-container_claims"]' to be visible
    And I expect the HTML element '[data-cy="field_claimsTemplate"]' to be visible
    And I expect the HTML element '[data-cy="field_claimsTemplate"]' to have value "{ \"sub\": \"id\" }"
    And I expect the HTML element '[data-cy="button_cancel"]' contains "Retour"
    And I expect the HTML element '[data-cy="button_confirm"]' contains "Enregistrer"

    ## 103 Should come back to details page when clicking on the cancel button
    When I click on '[data-cy="buttons-card"] [data-cy="button_cancel"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"

    ## 104 Submitting an empty form should display required validation messages
    When I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}/edit"

    When I clear the text in the HTML element "[data-cy='field_code']"
    And I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Ce champ est requis"
    When I set the text "new_value" in the HTML element "[data-cy='field_code']"
    Then I expect the HTML element '[role="alert"]' not exists

    When I clear the text in the HTML element "[data-cy='field_name']"
    And I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Ce champ est requis"
    When I set the text "new value" in the HTML element "[data-cy='field_name']"
    Then I expect the HTML element '[role="alert"]' not exists

    When I clear the text in the HTML element "[data-cy='field_type']"
    And I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Ce champ est requis"
    When I set the text "new value" in the HTML element "[data-cy='field_type']"
    Then I expect the HTML element '[role="alert"]' not exists

    When I clear the text in the HTML element "[data-cy='field_domain']"
    Then I expect the HTML element '[role="alert"]' not exists
    When I set the text "new value" in the HTML element "[data-cy='field_domain']"
    Then I expect the HTML element '[role="alert"]' not exists

    When I clear the text in the HTML element "[data-cy='field_description']"
    Then I expect the HTML element '[role="alert"]' not exists
    When I set the text "new value" in the HTML element "[data-cy='field_description']"
    Then I expect the HTML element '[role="alert"]' not exists

    When I clear the text in the HTML element "[data-cy='field_claimsTemplate']"
    And I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[role="alert"]' contains "Ce champ est requis"
    When I set the text "new value" in the HTML element "[data-cy='field_claimsTemplate']"
    Then I expect the HTML element '[role="alert"]' not exists

    ## 105 Should modify the application with the edition page
    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '.q-notification__message' contains "Application modifiée avec succès."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/applications/{{ctx.applicationId}}"
    And I expect the HTML element '[data-cy="generic-details-page"]' to be visible
    And I expect the HTML element '[data-cy="details-section_identity"]' to be visible
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--code"]' contains "new_value"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--name"]' contains "new value"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--description"]' contains "new value"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--domain"]' contains "new value"
    And I expect the HTML element '[data-cy="details-section_identity"] [data-cy="information-card--type"]' contains "new value"
    And I expect the HTML element '[data-cy="details-section_audit"]' to be visible
    And I expect the HTML element '[data-cy="details-section_audit"] [data-cy="information-card--createdBy"]' contains "admin_fn admin_ln"
    And I expect the HTML element '[data-cy="buttons-card"] [data-cy="button_cancel"]' contains "Retour"

    ####################################################
    ################## Cleanup and errors ##############
    ####################################################

    ## 106 Should display an error notification when navigating to a non-existent application
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/00000000-0000-4000-8000-000000000000/edit"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ## 107 Should display an error notification when navigating to an application with a malformed ID
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/not-a-valid-uuid/edit"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ## 108 Remove the application
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then I expect status code is 204
