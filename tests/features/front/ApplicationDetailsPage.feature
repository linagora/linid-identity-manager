Feature: Test Application details page display

  ################## Application Details ##################
  ## 101 Should display the application details page with its sections
  ## 102 Back button should come back at applications list page
  ## 103 Remove the application
  ## 104 Should display an error notification when navigating to a non-existent application
  ## 105 Should display an error notification when navigating to an application with a malformed ID

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

    ## 103 Remove the application
    When I request '{{env.E2E_API_URL}}/applications/{{ctx.applicationId}}' with method 'DELETE'
    Then I expect status code is 204

    ## 104 Should display an error notification when navigating to a non-existent application
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/00000000-0000-4000-8000-000000000000"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/applications"

    ## 105 Should display an error notification when navigating to an application with a malformed ID
    Given I visit the "{{ env.E2E_FRONT_URL }}/applications/not-a-valid-uuid"
    Then I expect the HTML element '.q-notification__message' contains "Impossible de charger l'application. Veuillez réessayer plus tard."
    And I expect current url is "{{ env.E2E_FRONT_URL }}/applications"
