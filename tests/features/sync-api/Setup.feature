Feature: Test sync-api setup

  ################## Database ##########################
  ## 101 Should connect sync-api to the linid database with the sync_api_linid_user account
  ## 102 Should let the sync_api_linid_user account read <relation>

  ####################################################
  ################## Database ##########################
  ####################################################

  Scenario: 101 - Should connect sync-api to the linid database with the sync_api_linid_user account
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "SELECT pid FROM pg_stat_activity WHERE datname = '{{env.E2E_DATABASE_NAME}}' AND usename = '{{env.E2E_DATABASE_SYNC_API_USER}}' LIMIT 1"
    Then  I expect 1 database results

  Scenario Outline: 102 - Should let the sync_api_linid_user account read <relation>
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_SYNC_API_USER}}" password "{{env.E2E_DATABASE_SYNC_API_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "SELECT count(*) FROM <relation>"
    Then  I expect 1 database results

    Examples:
      | relation                     |
      | accounts                     |
      | applications                 |
      | application_roles            |
      | account_payloads             |
      | computed_rights              |
      | account_computed_rights_view |
