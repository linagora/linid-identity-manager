Feature: Test Module Users Form Field Validation

  ################## Email field ######################
  #### 101 Validate email is mandatory
  #### 102 Validate email value must be valid email
  #### 103 Validate email value must end with .com

  ################## firstName field ##################
  #### 201 Validate firstName is mandatory
  #### 202 Validate firstName length must be at most 20 characters
  #### 203 Validate firstName length must be at least 3 characters

  ################## lastName field ###################
  #### 301 Validate lastName is mandatory
  #### 302 Validate lastName length must be at most 20 characters
  #### 303 Validate lastName length must be at least 3 characters

  ################# displayName field #################
  #### 401 Validate displayName is optional

  ################# role field ########################
  #### 501 Validate role is optional

  ################# enabled field #####################
  #### 601 Validate enabled is optional
  #### 602 Remove the user

  Scenario: Roundtrip on form field validation
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{env.E2E_FRONT_URL}}/moduleUsers/new'

    ####################################################
    ################## Email field #####################
    ####################################################

    #### 101 Validate email is mandatory
    Then I expect the HTML element '[data-cy="field_email"]' exists
    And I expect the HTML element '[data-cy="field-container_email"]' not contains "L'email est requis."

    When I click on '[data-cy="field_email"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_email"]' contains "L'email est requis."

    When I set the text "test@example.com" in the HTML element '[data-cy="field_email"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_email"]' not contains "L'email est requis."

    #### 102 Validate email value must be valid email
    When I set the text " " in the HTML element '[data-cy="field_email"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_email"]' contains "L'email doit être une adresse email valide."

    When I set the text "invalid_email" in the HTML element '[data-cy="field_email"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_email"]' contains "L'email doit être une adresse email valide."

    When I set the text "test@example.com" in the HTML element '[data-cy="field_email"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_email"]' not contains "L'email doit être une adresse email valide."

    #### 103 Validate email value must end with .com
    When I set the text "test@example.fr" in the HTML element '[data-cy="field_email"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_email"]' contains "Validation errors occurred for entity: user"

    When I set the text "test@example.com" in the HTML element '[data-cy="field_email"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_email"]' not contains "Validation errors occurred for entity: user"

    ####################################################
    ################ firstName field ###################
    ####################################################

    #### 201 Validate firstName is mandatory
    And I expect the HTML element '[data-cy="field_firstName"]' exists
    And I expect the HTML element '[data-cy="field-container_firstName"]' not contains "Le prénom est requis."

    When I click on '[data-cy="field_firstName"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_firstName"]' contains "Le prénom est requis."

    When I set the text "Test" in the HTML element '[data-cy="field_firstName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_firstName"]' not contains "Le prénom est requis."

    #### 202 Validate firstName length must be at most 20 characters
    When I set the text "ThisIsAVeryLongFirstNameExceedingLimit" in the HTML element '[data-cy="field_firstName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_firstName"]' contains "Le prénom ne peut pas dépasser 20 caractères."

    When I set the text "Test" in the HTML element '[data-cy="field_firstName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_firstName"]' not contains "Le prénom ne peut pas dépasser 20 caractères."

    #### 203 Validate firstName length must be at least 3 characters
    When I set the text "Te" in the HTML element '[data-cy="field_firstName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_firstName"]' contains "Le prénom doit comporter au moins 3 caractères."

    When I set the text "Test" in the HTML element '[data-cy="field_firstName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_firstName"]' not contains "Le prénom doit comporter au moins 3 caractères."

    ####################################################
    ################ lastName field ####################
    ####################################################

    #### 301 Validate lastName is mandatory
    And I expect the HTML element '[data-cy="field_lastName"]' exists
    And I expect the HTML element '[data-cy="field-container_lastName"]' not contains "Le nom est requis."

    When I click on '[data-cy="field_lastName"]'
    And I click on '[data-cy="title"]'
    Then I expect the HTML element '[data-cy="field-container_lastName"]' contains "Le nom est requis."

    When I set the text "User" in the HTML element '[data-cy="field_lastName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_lastName"]' not contains "Le nom est requis."

    #### 302 Validate lastName length must be at most 20 characters
    When I set the text "ThisIsAVeryLongLastNameExceedingLimit" in the HTML element '[data-cy="field_lastName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_lastName"]' contains "Le nom ne peut pas dépasser 20 caractères."

    When I set the text "User" in the HTML element '[data-cy="field_lastName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_lastName"]' not contains "Le nom ne peut pas dépasser 20 caractères."

    #### 303 Validate lastName length must be at least 3 characters
    When I set the text "Us" in the HTML element '[data-cy="field_lastName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_lastName"]' contains "Le nom doit comporter au moins 3 caractères."

    When I set the text "User" in the HTML element '[data-cy="field_lastName"]'
    And I click on '[data-cy="field_email"]'
    Then I expect the HTML element '[data-cy="field-container_lastName"]' not contains "Le nom doit comporter au moins 3 caractères."

    ####################################################
    ############### displayName field ##################
    ####################################################

    #### 401 Validate displayName is optional
    And I expect the HTML element '[data-cy="field_displayName"]' exists
    And I expect the HTML element '[data-cy="field-container_displayName"]' not contains "est requis."

    When I click on '[data-cy="field_displayName"]'
    And I click on '[data-cy="title"]'

    Then I expect the HTML element '[data-cy="field-container_displayName"]' not contains "est requis."
    And I set the text "Test User" in the HTML element '[data-cy="field_displayName"]'

    ####################################################
    ############### role field #########################
    ####################################################

    #### 501 Validate role is optional
    And I expect the HTML element '[data-cy="field_role"]' exists
    And I expect the HTML element '[data-cy="field-container_role"]' not contains "est requis."

    When I click on '[data-cy="field_role"]'
    And I click on '[data-cy="title"]'

    Then I expect the HTML element '[data-cy="field-container_role"]' not contains "est requis."
    And I set the text "role test" in the HTML element '[data-cy="field_role"]'

    ####################################################
    ############### enabled field ######################
    ####################################################

    #### 601 Validate enabled is optional
    And I expect the HTML element '[data-cy="field_enabled"]' exists
    And I expect the HTML element '[data-cy="field-container_enabled"]' not contains "est requis."

    When I click on '[data-cy="button_confirm"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="user-details-card"]' to be visible
    And I expect the HTML element '[data-cy="information-card--enabled"]' to be visible
    And I expect the HTML element '[data-cy="information-card--enabled"] [data-cy="value"]' not contains "true"
    And I expect the HTML element '[data-cy="information-card--enabled"] [data-cy="value"]' not contains "false"

    #### 602 Remove the user
    When I request '{{env.E2E_API_URL}}/api/users/00000000-0000-0000-0000-000000000007' with method 'DELETE'
    Then I expect status code is 204
