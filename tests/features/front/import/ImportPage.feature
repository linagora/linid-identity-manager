Feature: Test Import page

  ################## Import Button ##################
  ## 101 Users page should display import button
  ## 102 Should navigate to Import module

  ################## Import page ##################
  ################## Navigation ##################
  ## 201 Should redirect to User Module

  ################## Page Display ##################
  ## 301 Should display title, table and file input

  ################## File Loading ##################
  ## 401 Should display error on invalid file
  ## 402 Should display warning on empty file
  ## 403 Should display success on valid file
  ## 404 Should display valid row of file inside table

  ################## File Import and clean ##################
  ## 501 Should display error if no row was imported
  ## 502 Should clean row in error
  ## 503 Should display success if all row was imported
  ## 504 Should clean row in success
  ## 505 Should display warning if some row was imported
  ## 506 Should clean all row
  ## 507 Verify that imported user exists

  Scenario: Roundtrip to test import module
    Given I set the viewport size to 1920 px by 1080 px
    And I visit the '{{ env.E2E_FRONT_URL }}/moduleUsers'

  ####################################################
  ################## Import Button ###################
  ####################################################

  ## 101 Users page should display import button
    Then I expect the HTML element '[data-cy="button_import"]' to be visible
    And I expect the HTML element '[data-cy="button_import"]' contains 'Importer un utilisateur'

  ## 102 Should navigate to Import module
    When I click on '[data-cy="button_import"]'
    Then I expect current url is '{{ env.E2E_FRONT_URL }}/moduleUsers/moduleImport'
    And I expect the HTML element '[data-cy="module-import-title"]' to be visible
    And I expect the HTML element '[data-cy="module-import-title"]' contains 'Importation des données'

  ####################################################
  ################## Import page #####################
  ####################################################
  ## 201 Should redirect to User Module
    And I expect the HTML element '[data-cy="button_cancel"]' to be visible
    And I expect the HTML element '[data-cy="button_cancel"]' contains 'Annuler'

    When I click on '[data-cy="button_cancel"]'
    Then I expect current url is '{{ env.E2E_FRONT_URL }}/moduleUsers'
    And I expect the HTML element '[data-cy="module-user-title"]' to be visible
    And I expect the HTML element '[data-cy="module-user-title"]' contains 'Utilisateurs'

  ####################################################
  ################## Page Display ####################
  ####################################################

    When I click on '[data-cy="button_import"]'
    Then I expect current url is '{{ env.E2E_FRONT_URL }}/moduleUsers/moduleImport'

  ## 301 Should display title, table and file input
    And I expect the HTML element '[data-cy="module-import-title"]' to be visible
    And I expect the HTML element '[data-cy="module-import-title"]' contains 'Importation des données'
    And I expect the HTML element '[data-cy="file-row"]' appear 0 times on screen
    And I expect the HTML element '[data-cy="button_cancel"]' to be visible
    And I expect the HTML element '[data-cy="button_cancel"]' contains 'Annuler'
    And I expect the HTML element '[data-cy="button_clear"][disabled]' to be visible
    And I expect the HTML element '[data-cy="button_clear"]' contains 'Effacer...'
    And I expect the HTML element '[data-cy="button_confirm"][disabled]' to be visible
    And I expect the HTML element '[data-cy="button_confirm"]' contains 'Importer'

  ####################################################
  ################## File loading ####################
  ####################################################
  ## 401 Should display error on invalid file
    And I expect the HTML element '[data-cy="notify_load_success"]' not exists
    And I expect the HTML element '[data-cy="notify_load_warning"]' not exists
    And I expect the HTML element '[data-cy="notify_load_error"]' not exists

    When I set file input '[data-cy="field_import_files"]' with file '{{ env.E2E_LOCAL_FILES }}/invalid'
    Then I expect the HTML element '[data-cy="notify_load_error"]' to be visible

    ## 402 Should display warning on empty file
    And I expect the HTML element '[data-cy="notify_load_success"]' not exists
    And I expect the HTML element '[data-cy="notify_load_warning"]' not exists
    And I expect the HTML element '[data-cy="notify_load_error"]' not exists

    When I set file input '[data-cy="field_import_files"]' with file '{{ env.E2E_LOCAL_FILES }}/valid_empty.csv'
    Then I expect the HTML element '[data-cy="notify_load_warning"]' to be visible

  ## 403 Should display success on valid file
    And I expect the HTML element '[data-cy="notify_load_success"]' not exists
    And I expect the HTML element '[data-cy="notify_load_warning"]' not exists
    And I expect the HTML element '[data-cy="notify_load_error"]' not exists

    When I set file input '[data-cy="field_import_files"]' with file '{{ env.E2E_LOCAL_FILES }}/valid_import_success.csv'
    Then I expect the HTML element '[data-cy="notify_load_success"]' to be visible

  ## 404 Should display valid row of file inside table
    And I expect the HTML element '[data-cy="file-row"]' appear 2 times on screen
    And I expect the HTML element '[data-cy="cell-__status_1"]' contains "Prête à être importée"
    And I expect the HTML element '[data-cy="cell-__file_1"]' contains "valid_import_success.csv"
    And I expect the HTML element '[data-cy="cell-email_1"]' contains "test_csv_import_1@example.com"
    And I expect the HTML element '[data-cy="cell-firstName_1"]' contains "FN_CSV1"
    And I expect the HTML element '[data-cy="cell-lastName_1"]' contains "LN_CSV1"
    And I expect the HTML element '[data-cy="cell-displayName_1"]' contains "FN_CSV1 LN_CSV1"
    And I expect the HTML element '[data-cy="cell-role_1"]' contains "R_CSV1"
    And I expect the HTML element '[data-cy="cell-dateOfBirth_1"]' contains "01/01/1990"
    And I expect the HTML element '[data-cy="cell-__status_2"]' contains "Prête à être importée"
    And I expect the HTML element '[data-cy="cell-__file_2"]' contains "valid_import_success.csv"
    And I expect the HTML element '[data-cy="cell-email_2"]' contains "test_csv_import_2@example.com"
    And I expect the HTML element '[data-cy="cell-firstName_2"]' contains "FN_CSV2"
    And I expect the HTML element '[data-cy="cell-lastName_2"]' contains "LN_CSV2"
    And I expect the HTML element '[data-cy="cell-displayName_2"]' contains "FN_CSV2 LN_CSV2"
    And I expect the HTML element '[data-cy="cell-role_2"]' contains "R_CSV2"
    And I expect the HTML element '[data-cy="cell-dateOfBirth_2"]' contains "02/01/1990"

  ####################################################
  ################## File Import and clean ###########
  ####################################################

  ## 501 Should display error if no row was imported
    When I set file input '[data-cy="field_import_files"]' with file '{{ env.E2E_LOCAL_FILES }}/valid_import_error.csv'
    Then I expect the HTML element '[data-cy="cell-__status_1"]' contains "Prête à être importée"
    And I expect the HTML element '[data-cy="cell-__file_1"]' contains "valid_import_error.csv"

    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="notify_import_error"]' exists
    And I expect the HTML element '[data-cy="file-row"]' appear 1 times on screen
    And I expect the HTML element '[data-cy="cell-__status_1"]' contains "Erreur(s) lors de l'importation"

  ## 502 Should clean row in error
    When I click on '[data-cy="button_clear"]'
    Then I expect the HTML element '[data-cy="button_clear_error"]' exists

    When I click on '[data-cy="button_clear_error"]'
    Then I expect the HTML element '[data-cy="notify_clear_success"]' exists
    And I expect the HTML element '[data-cy="file-row"]' appear 0 times on screen

  ## 503 Should display success if all row was imported
    When I set file input '[data-cy="field_import_files"]' with file '{{ env.E2E_LOCAL_FILES }}/valid_import_success.csv'
    Then I expect the HTML element '[data-cy="file-row"]' appear 2 times on screen
    And I expect the HTML element '[data-cy="cell-__status_1"]' contains "Prête à être importée"
    And I expect the HTML element '[data-cy="cell-__file_1"]' contains "valid_import_success.csv"
    And I expect the HTML element '[data-cy="cell-__status_2"]' contains "Prête à être importée"
    And I expect the HTML element '[data-cy="cell-__file_2"]' contains "valid_import_success.csv"

    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="notify_import_success"]' exists
    And I expect the HTML element '[data-cy="file-row"]' appear 2 times on screen
    And I expect the HTML element '[data-cy="cell-__status_1"]' contains "Importée avec succès"
    And I expect the HTML element '[data-cy="cell-__status_2"]' contains "Importée avec succès"
    And I expect the HTML element '[data-cy="notify_import_success"]' not exists

  ## 504 Should clean row in success
    When I click on '[data-cy="button_clear"]'
    Then I expect the HTML element '[data-cy="button_clear_success"]' exists

    When I click on '[data-cy="button_clear_success"]'
    Then I expect the HTML element '[data-cy="notify_clear_success"]' exists
    And I expect the HTML element '[data-cy="file-row"]' appear 0 times on screen

  ## 505 Should display warning if some row was imported
    When I set file input '[data-cy="field_import_files"]' with file '{{ env.E2E_LOCAL_FILES }}/valid_import_warning.csv'
    Then I expect the HTML element '[data-cy="file-row"]' appear 2 times on screen
    And I expect the HTML element '[data-cy="cell-__status_1"]' contains "Prête à être importée"
    And I expect the HTML element '[data-cy="cell-__file_1"]' contains "valid_import_warning.csv"
    And I expect the HTML element '[data-cy="cell-__status_2"]' contains "Prête à être importée"
    And I expect the HTML element '[data-cy="cell-__file_2"]' contains "valid_import_warning.csv"

    When I click on '[data-cy="button_confirm"]'
    Then I expect the HTML element '[data-cy="notify_import_warning"]' exists
    And I expect the HTML element '[data-cy="file-row"]' appear 2 times on screen
    And I expect the HTML element '[data-cy="cell-__status_1"]' contains "Importée avec succès"
    And I expect the HTML element '[data-cy="cell-__status_2"]' contains "Erreur(s) lors de l'importation"

  ## 506 Should clean all row
    When I click on '[data-cy="button_clear"]'
    Then I expect the HTML element '[data-cy="button_clear_all"]' exists

    When I click on '[data-cy="button_clear_all"]'
    Then I expect the HTML element '[data-cy="notify_clear_success"]' exists
    And I expect the HTML element '[data-cy="file-row"]' appear 0 times on screen

  ## 507 Verify that imported user exists
    When I request '{{env.E2E_API_URL}}/api/users?email=test_csv_import_1@example.com' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is not empty
    And  I expect '{{response.body.content[0].email}}' is 'test_csv_import_1@example.com'
    And  I expect '{{response.body.content[0].firstName}}' is 'FN_CSV1'
    And  I expect '{{response.body.content[0].lastName}}' is 'LN_CSV1'
    And  I expect '{{response.body.content[0].displayName}}' is 'FN_CSV1 LN_CSV1'
    And  I expect '{{response.body.content[0].role}}' is 'R_CSV1'
    And  I expect '{{response.body.content[0].dateOfBirth}}' is '01/01/1990'

    When I request '{{env.E2E_API_URL}}/api/users/{{response.body.content[0].id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/api/users?email=test_csv_import_2@example.com' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is not empty
    And  I expect '{{response.body.content[0].email}}' is 'test_csv_import_2@example.com'
    And  I expect '{{response.body.content[0].firstName}}' is 'FN_CSV2'
    And  I expect '{{response.body.content[0].lastName}}' is 'LN_CSV2'
    And  I expect '{{response.body.content[0].displayName}}' is 'FN_CSV2 LN_CSV2'
    And  I expect '{{response.body.content[0].role}}' is 'R_CSV2'
    And  I expect '{{response.body.content[0].dateOfBirth}}' is '02/01/1990'

    When I request '{{env.E2E_API_URL}}/api/users/{{response.body.content[0].id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/api/users?email=test_csv_import_3@example.com' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].id}}' is not empty
    And  I expect '{{response.body.content[0].email}}' is 'test_csv_import_3@example.com'
    And  I expect '{{response.body.content[0].firstName}}' is 'FN_CSV3'
    And  I expect '{{response.body.content[0].lastName}}' is 'LN_CSV3'
    And  I expect '{{response.body.content[0].displayName}}' is 'FN_CSV3 LN_CSV3'
    And  I expect '{{response.body.content[0].role}}' is 'R_CSV3'
    And  I expect '{{response.body.content[0].dateOfBirth}}' is '03/01/1990'

    When I request '{{env.E2E_API_URL}}/api/users/{{response.body.content[0].id}}' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/api/users?firstName=FN_CSV4' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

    When I request '{{env.E2E_API_URL}}/api/users?firstName=FN_CSV5' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'
