Feature: Test API Advanced Search

  ################## Filter by Email #################
  ## 101 Should filter users by email
  ## 102 Should return empty results when email filter matches nothing

  ################## Filter by FirstName #############
  ## 201 Should filter users by firstName
  ## 202 Should filter users by firstName (case insensitive)
  ## 203 Should return empty results when firstName filter matches nothing

  ################## Filter by LastName ##############
  ## 301 Should filter users by lastName
  ## 302 Should return users filtered by lastName Roe

  ################## Combined Filters ################
  ## 401 Should filter users by multiple criteria (email and firstName)
  ## 402 Should filter users by all three criteria
  ## 403 Should return empty when combined filters match nothing

  ################## Pagination with Filters #########
  ## 501 Should return paginated results with filters

  ####################################################
  ################## Filter by Email #################
  ####################################################

  Scenario: Should filter users by email
    When I request '{{env.E2E_API_URL}}/api/users?email=john' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.content[0].email}}' contains 'john'

  Scenario: Should return empty results when email filter matches nothing
    When I request '{{env.E2E_API_URL}}/api/users?email=nonexistent@email.com' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

  ####################################################
  ################## Filter by FirstName #############
  ####################################################

  Scenario: Should filter users by firstName
    When I request '{{env.E2E_API_URL}}/api/users?firstName=Jane' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.content[0].firstName}}' is 'Jane'

  Scenario: Should filter users by firstName (case insensitive)
    When I request '{{env.E2E_API_URL}}/api/users?firstName=jane' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.content[0].firstName}}' is 'Jane'

  Scenario: Should return empty results when firstName filter matches nothing
    When I request '{{env.E2E_API_URL}}/api/users?firstName=Unknown' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

  ####################################################
  ################## Filter by LastName ##############
  ####################################################

  Scenario: Should filter users by lastName
    When I request '{{env.E2E_API_URL}}/api/users?lastName=Doe' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.content[0].lastName}}' is 'Doe'

  Scenario: Should return users filtered by lastName Roe
    When I request '{{env.E2E_API_URL}}/api/users?lastName=Roe' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.content[0].lastName}}' is 'Roe'

  ####################################################
  ################## Combined Filters ################
  ####################################################

  Scenario: Should filter users by multiple criteria (email and firstName)
    When I request '{{env.E2E_API_URL}}/api/users?email=alice&firstName=Alice' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.content}}' is not empty
    And  I expect '{{response.body.content[0].email}}' is 'alice.smith@example.com'
    And  I expect '{{response.body.content[0].firstName}}' is 'Alice'

  Scenario: Should filter users by all three criteria
    When I request '{{env.E2E_API_URL}}/api/users?email=alice&firstName=Alice&lastName=Smith' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].email}}' is 'alice.smith@example.com'

  Scenario: Should return empty when combined filters match nothing
    When I request '{{env.E2E_API_URL}}/api/users?firstName=John&lastName=Smith' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

  ####################################################
  ################## Pagination with Filters #########
  ####################################################

  Scenario: Should return paginated results with filters
    When I request '{{env.E2E_API_URL}}/api/users?email=example&size=2' with method 'GET'
    Then I expect status code is 206
    And  I expect '{{response.body.numberOfElements}}' is '2'
    And  I expect '{{response.body.pageable.pageSize}}' is '2'
