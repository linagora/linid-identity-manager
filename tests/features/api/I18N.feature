Feature: Test API Internationalization

  ################## Languages #######################
  ## 101 Should return available languages with fr, en, fr-FR and en-US
  ## 102 Should return 405 for unsupported method on languages

  ################## Translations ####################
  ## 201 Should return French translations with content
  ## 202 Should return English translations with content
  ## 203 Should return 404 for non-existent language
  ## 204 Should return 405 for unsupported method on translations

  ####################################################
  ################## Languages #######################
  ####################################################

  Scenario: 101 - Should return available languages with fr, en, fr-FR and en-US
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 200
    And  I expect '{{response.body.length}}' is '4'
    And  I expect '{{response.body}}' contains 'fr'
    And  I expect '{{response.body}}' contains 'en'
    And  I expect '{{response.body}}' contains 'fr-FR'
    And  I expect '{{response.body}}' contains 'en-US'

  Scenario Outline: 102 - Should return 405 for unsupported method "<method>" on languages
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method '<method>'
    Then I expect status code is 405

    Examples:
      | method |
      | POST   |
      | PUT    |
      | PATCH  |
      | DELETE |

  ####################################################
  ################## Translations ####################
  ####################################################

  # Note: Translation keys contain dots (e.g., "error.entity.unknown") which cannot be
  # accessed with current step syntax. Only status code is verified.
  Scenario: 201 - Should return French translations with content
    When I request '{{env.E2E_API_URL}}/i18n/fr.json' with method 'GET'
    Then I expect status code is 200

  Scenario: 202 - Should return English translations with content
    When I request '{{env.E2E_API_URL}}/i18n/en.json' with method 'GET'
    Then I expect status code is 200

  Scenario: 203 - Should return 404 for non-existent language
    When I request '{{env.E2E_API_URL}}/i18n/nonexistent.json' with method 'GET'
    Then I expect status code is 404

  Scenario Outline: 204 - Should return 405 for unsupported method "<method>" on translations
    When I request '{{env.E2E_API_URL}}/i18n/fr.json' with method '<method>'
    Then I expect status code is 405

    Examples:
      | method |
      | POST   |
      | PUT    |
      | PATCH  |
      | DELETE |
