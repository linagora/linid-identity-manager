Feature: Test I18nController

  #########################################################
  ###Should return available languages and translations####
  #########################################################

    Scenario: 101 - Request an access token using password grant
    # Encodage Basic Auth : "linid-im-client:linid-im-secret" → Base64
    Given I set http header "Authorization" with "Basic bGluaWQtaW0tY2xpZW50OmxpbmlkLWltLXNlY3JldA=="
    Given I set http header "Content-Type" with "application/x-www-form-urlencoded"
    When I request "https://localhost:9000/auth/oauth2/token" with method "POST" with body:
    """
    grant_type=password&username=admin&password=password&scope=openid email profile roles
    """
    Then I expect status code is 200
    Then I log "{{ response.body.access_token }}"

    Given I set http header "Authorization" with "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6ImF0K0pXVCJ9.eyJlbWFpbCI6ImFkbWluQGV4YW1wbGUuY29tIiwicm9sZXMiOiJhZG1pbix1c2VyIiwiYXVkIjpbImxpbmlkLWltLWNsaWVudCJdLCJjbGllbnRfaWQiOiJsaW5pZC1pbS1jbGllbnQiLCJleHAiOjE3NzU1NjA0NzMsIm5hbWUiOiJhZG1pbiBuYW1lIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3NzU1NTY4NzMsInNpZCI6IllFd1VQU0toQUp0elRuNktXVE1XdzNUMWxXVjlaajJid0kwZVpXNjh1VHMiLCJqdGkiOiI4ZGNhMTNjNDY3MTM1MTUyZTE4OTAwMDNhZDYyNjAzYjc4ZTFhZmIzMmIxNmQyMjNmZDM3YWQ0OGI4ZGQ3OTVkIiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTAwMC9hdXRoIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSByb2xlcyIsInByZWZlcnJlZF91c2VybmFtZSI6ImFkbWluIn0.YFrCT0CWOVGYB1ZtSh0KEuUT_NztfBhw8jAKnIA_WtE_y7T0F2bAa1y6SOxY13oN7mMgDuryd70Cd66QKyZ3LZhaXcoykVyL5pOjrsDTn3rbRV5Pee9wPbgNpXEqRKSrMERnis7_U7Nc5upVWsc8hyyHXnK70_MWiT1AZmagsMuMWiRUeXmtrkITrAYaZ0_knvtKcv7SYwESrc6-dRKuEbxWeo68Jlu9swBQPdxeCyngTHurcKiohKwwVIrUd7fwufBDbenfZtyr02-RPH3T91vr_fXuIlZNKaZGXSBkXRir34QOjP5cIVCdwR8r-rRYNCEACHX9cVh9mB3vMG9l2Q"
#    Given I set http header "Authorization" with "Bearer {{ response.body.access_token }}"
    When I request '{{env.E2E_API_URL}}/i18n/languages' with method 'GET'

    And I expect status code is 200
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
      Then I expect status code is 401
