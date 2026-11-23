Feature: Test the SQL functions reading the RLS context

  # Note: the API sets the RLS context of each transaction with set_config(..., true), as these scenarios do:
  #       the variables only last for the current transaction.
  # The database steps keep a single connection for the whole scenario: once the transaction ends, the scenario
  # reuses this connection, like the next request of the API using the same connection of the pool.

  ################## RLS context ##################
  ## 101 Should return no account and no SUPER_ADMIN flag when no context is set
  ## 102 Should return the account of the user inside the transaction, and nothing once it is committed
  ## 103 Should return full access without account in a system context, and nothing once it is rolled back

  ######################################################################
  ################## RLS context #######################################
  ######################################################################

  Scenario: 101 - Should return no account and no SUPER_ADMIN flag when no context is set
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "SELECT current_account_id() IS NULL AS no_account_id, current_account_id()::TEXT AS account_id, is_super_admin() AS super_admin"
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].no_account_id}}' is 'true'
    And   I expect '{{ctx.dbResults[0].super_admin}}' is 'false'

  Scenario: 102 - Should return the account of the user inside the transaction, and nothing once it is committed
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "BEGIN"
    And   I execute sql request "SELECT set_config('linid.account_id', $1, true), set_config('linid.is_super_admin', $2, true), set_config('linid.super_admin_sub', $3, true)" with values:
      """
      ["00000000-0000-4000-8000-00000000a002", "false", "jdoe"]
      """
    And   I execute sql request "SELECT current_account_id() IS NULL AS no_account_id, current_account_id()::TEXT AS account_id, is_super_admin() AS super_admin"
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].account_id}}' is '00000000-0000-4000-8000-00000000a002'
    And   I expect '{{ctx.dbResults[0].super_admin}}' is 'false'

    When  I execute sql request "COMMIT"
    And   I execute sql request "SELECT current_account_id() IS NULL AS no_account_id, current_account_id()::TEXT AS account_id, is_super_admin() AS super_admin"
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].no_account_id}}' is 'true'
    And   I expect '{{ctx.dbResults[0].super_admin}}' is 'false'

  Scenario: 103 - Should return full access without account in a system context, and nothing once it is rolled back
    Given I setup database with driver "postgres" host "{{env.E2E_DATABASE_HOST}}" port 5432 user "{{env.E2E_DATABASE_ADMIN_USER}}" password "{{env.E2E_DATABASE_ADMIN_PASSWORD}}" database "{{env.E2E_DATABASE_NAME}}"
    When  I execute sql request "BEGIN"
    And   I execute sql request "SELECT set_config('linid.account_id', $1, true), set_config('linid.is_super_admin', $2, true), set_config('linid.super_admin_sub', $3, true)" with values:
      """
      ["", "true", "jdoe"]
      """
    And   I execute sql request "SELECT current_account_id() IS NULL AS no_account_id, current_account_id()::TEXT AS account_id, is_super_admin() AS super_admin"
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].no_account_id}}' is 'true'
    And   I expect '{{ctx.dbResults[0].super_admin}}' is 'true'

    When  I execute sql request "ROLLBACK"
    And   I execute sql request "SELECT current_account_id() IS NULL AS no_account_id, current_account_id()::TEXT AS account_id, is_super_admin() AS super_admin"
    Then  I expect 1 database results
    And   I expect '{{ctx.dbResults[0].no_account_id}}' is 'true'
    And   I expect '{{ctx.dbResults[0].super_admin}}' is 'false'
