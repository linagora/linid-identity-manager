Feature: Test I18nController

  #########################################################
  ###Should return available languages and translations####
  #########################################################

  Background: Request an access token using password grant
    # Encodage Basic Auth : "linid-im-client:linid-im-secret" → Base64
    Given I set http header "Authorization" with "Basic bGluaWQtaW0tY2xpZW50OmxpbmlkLWltLXNlY3JldA=="
    Given I set http header "Content-Type" with "application/x-www-form-urlencoded"
    When I request "{{env.E2E_FRONT_URL}}/auth/oauth2/token" with method "POST" with body:
    """
    grant_type=password&username=admin&password=password&scope=openid email profile roles
    """
    Then I expect status code is 200
    Given I set http header "Authorization" with "Bearer {{ response.body.access_token }}"
    And I set http header "Content-Type" with "application/json"

  Scenario: 101 - Should return available languages
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'
    Then I expect status code is 200
    And I expect one resource of '{{response.body | json}}' equals to 'en'

  Scenario: 102 - Should return a translation file for a known language
    When I request '{{env.E2E_API_URL}}/i18n/en.json' with method 'GET'
    Then I expect status code is 200

  Scenario: 103 - Should return 404 for unknown language
    When I request '{{env.E2E_API_URL}}/i18n/zz.json' with method 'GET'
    Then I expect status code is 404
