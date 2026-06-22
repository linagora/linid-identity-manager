Feature: Tests API Users Preferences endpoints

  ################## Create or Update (POST /user-preferences) ##
  ## 101 Should create a preference with valid data and return key/value
  ## 102 Should return 400 with invalid data
  ## 103 Should update value when posting an existing key (upsert)

  ################## Delete (DELETE /user-preferences/{key}) ####
  ## 201 Should return 204 when deleting an existing key and no longer return it on GET
  ## 202 Should return 404 when deleting an unknown key

  ################## Get All (GET /user-preferences) ############
  ## 301 Should return empty object when no preferences exist
  ## 302 Should return all preferences after creates and after updates

  Background:
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

  ###############################################################
  ################## Create or Update (POST /user-preferences) ##
  ###############################################################

  Scenario: 101 - Should create a preference with valid data and return key/value
    When  I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      {
        "key": "theme",
        "value": "dark"
      }
      """
    Then  I expect status code is 201
    And   I expect '{{response.body | dump}}' as 'json' to have length 2
    And   I expect '{{response.body.key}}' is 'theme'
    And   I expect '{{response.body.value}}' is 'dark'

    When  I request '{{env.E2E_API_URL}}/user-preferences/{{response.body.key}}' with method 'DELETE'
    Then  I expect status code is 204

  Scenario Outline: 102 - Should return 400 with invalid data <case>
    When  I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      <body>
      """
    Then  I expect status code is 400

    Examples:
      | case          | body                                    |
      | empty key     | { "key": "", "value": "dark" }          |
      | digit in key  | { "key": "theme1", "value": "dark" }    |
      | space in key  | { "key": "bad key", "value": "dark" }   |
      | dash leading  | { "key": "-theme", "value": "dark" }    |
      | dash trailing | { "key": "theme-", "value": "dark" }    |
      | dash double   | { "key": "the--me", "value": "dark" }   |
      | missing key   | { "value": "dark" }                     |
      | missing value | { "key": "theme" }                      |

  Scenario: 103 - Should update value when posting an existing key (upsert)
    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      {
        "key": "language",
        "value": "french"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.value}}' is 'french'

    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      {
        "key": "language",
        "value": "english"
      }
      """
    Then I expect status code is 201
    And  I expect '{{response.body.value}}' is 'english'

    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body | dump}}' as 'json' to have length 1
    And  I expect '{{response.body.language}}' is 'english'

    When I request '{{env.E2E_API_URL}}/user-preferences/language' with method 'DELETE'
    Then I expect status code is 204

  ###############################################################
  ################## Delete (DELETE /user-preferences/{key}) ####
  ###############################################################

  Scenario: 201 - Should return 204 when deleting an existing key and no longer return it on GET
    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      {
        "key": "theme_to_delete",
        "value": "dark"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body | dump}}' as 'json' to have length 1

    When I request '{{env.E2E_API_URL}}/user-preferences/theme_to_delete' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body | dump}}' as 'json' to have length 0

  Scenario: 202 - Should return 404 when deleting an unknown key
    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      {
        "key": "test",
        "value": "x"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/user-preferences/test' with method 'DELETE'
    Then I expect status code is 204

    When I request '{{env.E2E_API_URL}}/user-preferences/test' with method 'DELETE'
    Then I expect status code is 404

  ###############################################################
  ################## Get All (GET /user-preferences) ############
  ###############################################################

  Scenario: 301 - Should return empty object when no preferences exist
    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body | dump}}' as 'json' to have length 0

  Scenario: 302 - Should return all preferences after creates and after updates
    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      {
        "key": "theme",
        "value": "dark"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.theme}}' is 'dark'

    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'POST' with body:
      """
      {
        "key": "theme",
        "value": "light"
      }
      """
    Then I expect status code is 201

    When I request '{{env.E2E_API_URL}}/user-preferences' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.theme}}' is 'light'

    When I request '{{env.E2E_API_URL}}/user-preferences/theme' with method 'DELETE'
    Then I expect status code is 204
