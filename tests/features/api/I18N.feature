Feature: Test I18nController

  ## 101 Should not return available languages when having bad token
  ## 102 Should get access token
  ## 103 Should return available languages
  ## 104 Should return a translation file for a known language (english)
  ## 105 Should return a translation file for a known language (french)
  ## 106 Should not return a translation file for an unknown language (italian)
  Scenario: Test all endpoints of i18NController

  #########################################################
  ################ Tests i18N endpoints ###################
  #########################################################

  ## 101 Should not return available languages when having bad token
    Given I set http header 'Authorization' with 'Bearer badtoken'
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 401

  ## 102 Should get access token
    Given I set http header 'Authorization' with '{{ env.E2E_AUTH_TOKEN }}'
    And I set http header 'Content-Type' with 'application/x-www-form-urlencoded'
    When I request '{{env.E2E_AUTH_URL}}/oauth2/token' with method 'POST' with body:
      """
      grant_type=password&username=admin&password=password&scope=openid email profile roles
      """
    Then I expect status code is 200
    And I expect '{{ response.body.access_token }}' is not empty
    And I store 'accessToken' as '{{response.body.access_token}}' in context

  ## 103 Should return available languages
    Given I set http header 'Authorization' with 'Bearer {{ctx.accessToken}}'
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 200
    And I expect '{{response.body | json}}' contains 'en'
    And I expect '{{response.body | json}}' contains 'fr'

  ## 104 Should return a translation file for a known language (english)
    When I request '{{env.E2E_API_URL}}/i18n/en.json' with method 'GET'
    Then I expect status code is 200
    # We check that at least one of the translation exists
    And I expect '{{response.body | json}}' contains '"error.entity.attributes": "Validation errors occurred for entity: {entity}"'

  ## 105 Should return a translation file for a known language (french)
    When I request '{{env.E2E_API_URL}}/i18n/fr.json' with method 'GET'
    Then I expect status code is 200
    # We check that at least one of the translation exists
    And I expect '{{response.body | json}}' contains '"error.entity.attributes": "Erreurs de validation pour l\'entité: {entity}"'

  ## 106 Should not return a translation file for an unknown language (italian)
    When I request '{{env.E2E_API_URL}}/i18n/it.json' with method 'GET'
    Then I expect status code is 404
    And I expect '{{response.body.error}}' contains 'Unknown language: it'
