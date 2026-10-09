Feature: Test API Avatar endpoints

  # Note: Background handles authentication before each Scenario.
  # The upload of an image needs a multipart request, which the runner cannot send: it is covered by the
  # account details page feature through the front-end.

  ################## Upload (POST /avatars/{entity}/{id}) ##############
  ## 101 Should return 415 when the upload is not a multipart request

  ################## Delete (DELETE /avatars/{entity}/{id}) ############
  ## 201 Should return 204 when deleting the avatar of an account without avatar
  ## 202 Should return 204 when deleting the avatar of an application without avatar
  ## 203 Should return 204 when deleting the avatar of an organizational unit without avatar
  ## 204 Should return 204 when deleting the avatar of a group without avatar
  ## 205 Should return 404 when deleting the avatar of an unknown account
  ## 206 Should return 400 when deleting the avatar of an unsupported entity type

  ################## Serving (GET /avatars/{entity}/{id}.png) ##########
  ## 301 Should serve the default administrator avatar as a sandboxed image

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

  ####################################################
  ################## Upload (POST /avatars/{entity}/{id}) ##############
  ####################################################

  Scenario: 101 - Should return 415 when the upload is not a multipart request
    When I request '{{env.E2E_API_URL}}/avatars/accounts/00000000-0000-4000-8000-00000000a002' with method 'POST' with body:
      """
      {}
      """
    Then I expect status code is 415

  ####################################################
  ################## Delete (DELETE /avatars/{entity}/{id}) ############
  ####################################################

  Scenario: 201 - Should return 204 when deleting the avatar of an account without avatar
    When I request '{{env.E2E_API_URL}}/avatars/accounts/00000000-0000-4000-8000-00000000a002' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 202 - Should return 204 when deleting the avatar of an application without avatar
    When I request '{{env.E2E_API_URL}}/applications?code=LINID' with method 'GET'
    Then I expect status code is 200
    And  I store 'linidApplicationId' as '{{response.body.content[0].id}}' in context

    When I request '{{env.E2E_API_URL}}/avatars/applications/{{ctx.linidApplicationId}}' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 203 - Should return 204 when deleting the avatar of an organizational unit without avatar
    When I request '{{env.E2E_API_URL}}/avatars/organizational-units/00000000-0000-4000-8000-00000000000a' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 204 - Should return 204 when deleting the avatar of a group without avatar
    When I request '{{env.E2E_API_URL}}/avatars/groups/00000000-0000-4000-8000-000000009001' with method 'DELETE'
    Then I expect status code is 204

  Scenario: 205 - Should return 404 when deleting the avatar of an unknown account
    When I request '{{env.E2E_API_URL}}/avatars/accounts/00000000-0000-0000-0000-000000000000' with method 'DELETE'
    Then I expect status code is 404
    And  I expect '{{response.body.errorKey}}' is 'error.account.not_found'

  Scenario: 206 - Should return 400 when deleting the avatar of an unsupported entity type
    When I request '{{env.E2E_API_URL}}/avatars/roles/00000000-0000-4000-8000-00000000a002' with method 'DELETE'
    Then I expect status code is 400
    And  I expect '{{response.body.errorKey}}' is 'error.avatar.entity.unsupported'

  ####################################################
  ################## Serving (GET /avatars/{entity}/{id}.png) ##########
  ####################################################

  Scenario: 301 - Should serve the default administrator avatar as a sandboxed image
    When I request '{{env.E2E_FRONT_URL}}/avatars/accounts/00000000-0000-4000-8000-00000000a001.png' with method 'GET'
    Then I expect status code is 200
    And  I expect http header 'content-type' is 'image/png'
    And  I expect http header 'x-content-type-options' is 'nosniff'
    And  I expect http header 'content-security-policy' is "default-src 'none'; sandbox"
    And  I expect http header 'cache-control' is 'no-cache'
