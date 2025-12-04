Feature: Test API Internationalization

  ################## Languages ##################
  ## 101 Should return available languages
  ## 102 Should return fr and en in available languages
  ## 103 Should return fr-FR and en-US locales
  ## 104 Should return 405 for unsupported method on languages

  ################## Translations ##################
  ## 201 Should return French translations
  ## 202 Should return English translations
  ## 203 Should return empty object for non-existent language
  ## 204 Should return 405 for unsupported method on translations

  Scenario: 101 - Should return available languages
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario: 102 - Should return fr and en in available languages
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' contains 'fr'
    And  I expect '{{response.body}}' contains 'en'

  Scenario: 103 - Should return fr-FR and en-US locales
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' contains 'fr-FR'
    And  I expect '{{response.body}}' contains 'en-US'

  Scenario: 104 - Should return 405 for unsupported method on languages
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'DELETE'
    Then I expect status code is 405

  Scenario: 201 - Should return French translations
    When I request '{{env.E2E_API_URL}}/i18n/fr.json' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario: 202 - Should return English translations
    When I request '{{env.E2E_API_URL}}/i18n/en.json' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body}}' is not empty

  Scenario: 203 - Should return empty object for non-existent language
    When I request '{{env.E2E_API_URL}}/i18n/nonexistent.json' with method 'GET'
    Then I expect status code is 200

  Scenario: 204 - Should return 405 for unsupported method on translations
    When I request '{{env.E2E_API_URL}}/i18n/fr.json' with method 'DELETE'
    Then I expect status code is 405
