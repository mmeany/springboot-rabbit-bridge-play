Feature:

  Background:
    * url rabbit_bridge.url

  Scenario: start-listening
    Given path 'start-listening'
    And request
      """
      {
        "queueNames": ["red", "amber", "green"]
      }
      """
    When method post
    Then status 200

  Scenario: stop-listening
    Given path 'stop-listening'
    When method post
    Then status 200

  Scenario: reset
    Given path 'reset'
    When method post
    Then status 200