Feature: Test API Advanced Search

  ################## Filter by Email #################
  ## 101 Should filter users by email
  ## 102 Should return empty results when email filter matches nothing

  ################## Filter by FirstName #############
  ## 201 Should filter users by firstName (case insensitive)
  ## 202 Should return empty results when firstName filter matches nothing

  ################## Filter by LastName ##############
  ## 301 Should filter users by lastName

  ################## Combined Filters ################
  ## 401 Should filter users by multiple criteria (email and firstName)
  ## 402 Should filter users by all three criteria
  ## 403 Should return empty when combined filters match nothing

  ################## Pagination with Filters #########
  ## 501 Should return paginated results with filters

  ####################################################
  ################## Filter by Email #################
  ####################################################

  Scenario: 101 - Should filter users by email
    When I request '{{env.E2E_API_URL}}/api/users?email=john.doe' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].email}}' is 'john.doe@example.com'

  Scenario: 102 - Should return empty results when email filter matches nothing
    When I request '{{env.E2E_API_URL}}/api/users?email=nonexistent@email.com' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

  ####################################################
  ################## Filter by FirstName #############
  ####################################################

  Scenario Outline: 201 - Should filter users by firstName "<search>" (case insensitive)
    When I request '{{env.E2E_API_URL}}/api/users?firstName=<search>' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].firstName}}' is '<firstName>'
    And  I expect '{{response.body.content[0].email}}' is '<email>'

    Examples:
      | search | firstName | email                |
      | Jane   | Jane      | jane.roe@example.com |
      | jane   | Jane      | jane.roe@example.com |

  Scenario: 202 - Should return empty results when firstName filter matches nothing
    When I request '{{env.E2E_API_URL}}/api/users?firstName=Unknown' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

  ####################################################
  ################## Filter by LastName ##############
  ####################################################

  Scenario Outline: 301 - Should filter users by lastName "<search>"
    When I request '{{env.E2E_API_URL}}/api/users?lastName=<search>' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '<count>'
    And  I expect '{{response.body.content[0].lastName}}' is '<lastName>'
    And  I expect '{{response.body.content[0].email}}' is '<email>'

    Examples:
      | search | count | lastName | email                |
      | Doe    | 2     | Doe      | john.doe@example.com |
      | Roe    | 1     | Roe      | jane.roe@example.com |

  ####################################################
  ################## Combined Filters ################
  ####################################################

  Scenario: 401 - Should filter users by multiple criteria (email and firstName)
    When I request '{{env.E2E_API_URL}}/api/users?email=alice&firstName=Alice' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].email}}' is 'alice.smith@example.com'
    And  I expect '{{response.body.content[0].firstName}}' is 'Alice'

  Scenario: 402 - Should filter users by all three criteria
    When I request '{{env.E2E_API_URL}}/api/users?email=alice&firstName=Alice&lastName=Smith' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '1'
    And  I expect '{{response.body.content[0].email}}' is 'alice.smith@example.com'

  Scenario: 403 - Should return empty when combined filters match nothing
    When I request '{{env.E2E_API_URL}}/api/users?firstName=John&lastName=Smith' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.totalElements}}' is '0'

  ####################################################
  ################## Pagination with Filters #########
  ####################################################

  Scenario: 501 - Should return paginated results with filters
    When I request '{{env.E2E_API_URL}}/api/users?email=example&size=2' with method 'GET'
    Then I expect status code is 206
    And  I expect '{{response.body.numberOfElements}}' is '2'
    And  I expect '{{response.body.pageable.pageSize}}' is '2'
