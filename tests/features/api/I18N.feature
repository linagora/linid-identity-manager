Feature: Test I18nController

  ################## Tests i18N endpoints #######################
  ## 101 Should return available languages
  ## 102 Should return a translation file for a known language (english)
  ## 103 Should return a translation file for a known language (french)
  ## 104 Should return an empty translation file for an unknown language (italian)

  #########################################################
  ################ Tests i18N endpoints ###################
  #########################################################

  Scenario: 101 - Should return available languages
    # No 'Authorization' header is set — httpHeaders context is reset before each scenario
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 200
    And I expect '{{response.body | json}}' contains 'en'
    And I expect '{{response.body | json}}' contains 'fr'

  Scenario: 102 - Should return a translation file for a known language (english)
    When I request '{{env.E2E_API_URL}}/i18n/en.json' with method 'GET'
    Then I expect status code is 200
    # We check that at least one of the translation exists
    And I expect '{{response.body | json}}' contains '"error.entity.attributes": "Validation errors occurred for entity: {entity}"'

  Scenario: 103 - Should return a translation file for a known language (french)
    When I request '{{env.E2E_API_URL}}/i18n/fr.json' with method 'GET'
    Then I expect status code is 200
    # We check that at least one of the translation exists
    And I expect '{{response.body | json}}' contains '"error.entity.attributes": "Erreurs de validation pour l\'entité: {entity}"'

  Scenario: 104 - Should return an empty translation file for an unknown language (italian)
    When I request '{{env.E2E_API_URL}}/i18n/it.json' with method 'GET'
    Then I expect status code is 200
    And I expect '{{response.body | json}}' is '{}' as 'json'
