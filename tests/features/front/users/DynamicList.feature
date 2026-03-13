Feature: Test Dynamic List from Mock API

  ################## Dynamic List Dropdown ##################
  ## 101 Should display status field as DynamicList on creation page
  ## 102 Should retrieve status list from Mock API
  ## 103 Should populate dropdown with values from API
  ## 104 Should allow selecting a status value

  ################## Create User with Status ##################
  ## 201 Should successfully create user with selected status
  ## 202 Should display success notification after creation
  ## 203 Should redirect to detail page after creation
  ## 204 Should display selected status value on detail page

  ################## Edit User Status ##################
  ## 301 Should display current status on edit page
  ## 302 Should pre-fill status field with current value
  ## 303 Should allow changing status to a different value
  ## 304 Should display success notification after update
  ## 305 Should persist updated status on detail page

  ################## API Verification ##################
  ## 401 Should return status field in API response

  ################## Cleanup ##################
  ## 501 Remove the test user

  Scenario: Complete dynamic list workflow for status field
    Given I set the viewport size to 1920 px by 1080 px

    ####################################################
    ################## Dynamic List Dropdown ############
    ####################################################

    ## 101 Should display status field as DynamicList on creation page
    When I visit the '{{ env.E2E_FRONT_URL }}/moduleUsers/new'
    Then I expect the HTML element '[data-cy="new-user-page"]' to be visible
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_status"]' to be visible

    ## 102 Should retrieve status list from Mock API
    When I request '{{ env.E2E_API_URL }}/options/statuses?page=0&size=200' with method 'GET'
    Then I expect status code is 200
    And I expect '{{ response.body.content }}' is not empty
    And I store 'firstStatusLabel' as '{{ response.body.content[0].label }}' in context
    And I store 'firstStatusValue' as '{{ response.body.content[0].value }}' in context
    And I store 'secondStatusLabel' as '{{ response.body.content[1].label }}' in context
    And I store 'secondStatusValue' as '{{ response.body.content[1].value }}' in context

    ## 103 Should populate dropdown with values from API
    When I click on '[data-cy="field_status"]'
    Then I expect the HTML element '.q-virtual-scroll__content .q-item' to be visible

    ## 104 Should allow selecting a status value
    When I click on '.q-virtual-scroll__content > :first-child'
    Then I expect the HTML element '.q-menu' not exists

    ####################################################
    ################## Create User with Status ##########
    ####################################################

    ## 201 Should successfully create user with selected status
    When I set the text "dynlist.test@example.com" in the HTML element '[data-cy="field_email"]'
    And I set the text "Dynamic" in the HTML element '[data-cy="field_firstName"]'
    And I set the text "ListTest" in the HTML element '[data-cy="field_lastName"]'
    And I select ".q-item:nth-child(3)" in "[data-cy='field_roleWithInvalidDefaultValue']"
    And I select ".q-virtual-scroll__content > :first-child" in "[data-cy='field_status']"
    And I expect the HTML element '.q-menu' not exists
    And I expect the HTML element '[data-cy="field-container_status"]' contains "{{ ctx.firstStatusLabel }}"
    And I click on '[data-cy="button_confirm"]'

    ## 202 Should display success notification after creation
    Then I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Utilisateur créé avec succès"

    ## 203 Should redirect to detail page after creation
    And I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="user-details-page_title"]' to be visible

    ## 204 Should display selected status value on detail page
    And I expect the HTML element '[data-cy="information-card--status"]' to be visible
    And I expect the HTML element '[data-cy="information-card--status"] [data-cy="value"]' contains "{{ ctx.firstStatusValue }}"

    ####################################################
    ################## Edit User Status ################
    ####################################################

    ## 301 Should display current status on edit page
    When I click on '[data-cy="buttons-card"] [data-cy="button_edit"]'
    Then I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007/edit"
    And I expect the HTML element '[data-cy="title"]' contains "Édition de l'utilisateur"
    And I expect the HTML element '[data-cy="form-section-card_secondary"] [data-cy="field-container_status"]' to be visible

    ## 302 Should pre-fill status field with current value
    And I expect the HTML element '[data-cy="field-container_status"]' contains "{{ ctx.firstStatusLabel }}"

    ## 303 Should allow changing status to a different value
    When I select ".q-virtual-scroll__content > :nth-child(2)" in "[data-cy='field_status']"
    Then I expect the HTML element '.q-menu' not exists
    And I expect the HTML element '[data-cy="field-container_status"]' contains "{{ ctx.secondStatusLabel }}"
    And I click on '[data-cy="button_confirm"]'

    ## 304 Should display success notification after update
    And I expect the HTML element ".q-notification__message" to be visible
    And I expect the HTML element ".q-notification__message" contains "Utilisateur mis à jour avec succès"

    ## 305 Should persist updated status on detail page
    And I expect current url is "{{ env.E2E_FRONT_URL }}/moduleUsers/00000000-0000-0000-0000-000000000007"
    And I expect the HTML element '[data-cy="user-details-page_title"]' to be visible
    And I expect the HTML element '[data-cy="information-card--status"] [data-cy="value"]' contains "{{ ctx.secondStatusValue }}"

    ####################################################
    ################## API Verification ################
    ####################################################

    ## 401 Should return status field in API response
    When I request '{{ env.E2E_API_URL }}/api/users/00000000-0000-0000-0000-000000000007' with method 'GET'
    Then I expect status code is 200
    And I expect '{{ response.body.status }}' is not empty

    ####################################################
    ################## Cleanup #########################
    ####################################################

    ## 501 Remove the test user
    When I request '{{ env.E2E_API_URL }}/api/users/00000000-0000-0000-0000-000000000007' with method 'DELETE'
    Then I expect status code is 204
