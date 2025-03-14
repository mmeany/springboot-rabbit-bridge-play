Feature: And check messages by reading expected content from a CSV file

  Background:
    * print rabbit_bridge.url
    * url rabbit_bridge.url
    # Pull in all schema definitions from the schemas.feature file, these can be used to validate responses
    * callonce read('../helpers/schemas.feature')

  Scenario: Start listening
    Given path 'start-listening'
    And request
      """
      {
        "queueNames": ["red", "amber", "green"]
      }
      """
    When method post
    Then status 200

  Scenario Outline: Post messages - messages come from a CSV file
    Given path 'send'
    And request
      """
      {
        "queueName": "<queueName>",
        "message": "<message>"
      }
      """
    When method post
    Then status 200

    Examples:
      | read('../examples/sample-messages.csv') |

  Scenario: Stop listening
    Given path 'stop-listening'
    When method post
    Then status 200

  Scenario Outline: Check results - expected results are read from a CSV file
    # <queueName>, <index>, <id> and <message> are variables from the Examples table (CSV file)
    Given path 'message', "<queueName>", "<index>"
    When method get
    Then status 200
    And match response.id == parseInt("<id>")
    And match response.queueName == "<queueName>"
    And match response.message == "<message>"
    And match response == MESSAGE_SCHEMA

    Examples:
      | read('../examples/sample-messages-from-queues.csv') |

  Scenario: Reset the bridge
    Given path 'reset'
    When method post
    Then status 200