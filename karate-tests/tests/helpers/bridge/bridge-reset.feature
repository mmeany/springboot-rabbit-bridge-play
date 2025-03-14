Feature:

  Background:
    * url rabbit_bridge.url

  Scenario: reset
    Given path 'reset'
    When method post
    Then status 200
