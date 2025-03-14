Feature:

  Background:
    * url rabbit_bridge.url

  Scenario: stop-listening
    Given path 'stop-listening'
    When method post
    Then status 200
