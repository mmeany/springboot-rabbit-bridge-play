Feature:

  Background:
    * url rabbit_bridge.url

  Scenario: start-listening
    # Make sure the bridge is reset before starting to listen
    Given path 'reset'
    When method post
    Then status 200

    # Start listening
    Given path 'start-listening'
    And request
      """
      {
        "queueNames": ["red", "amber", "green"]
      }
      """
    When method post
    Then status 200
