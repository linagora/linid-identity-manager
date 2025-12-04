Feature: Test homepage

    Scenario: Visit homepage
        Given I visit the '{{ env.E2E_FRONT_URL }}'
        Then I expect the HTML element '[data-cy="home-page"] [data-cy="title"]' contains 'LinID Identity Manager'
