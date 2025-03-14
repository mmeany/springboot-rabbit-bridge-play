Feature: Finally, use common steps to control the bridge

  # Finally, a complete example that:
  # - Uses a CSV file as a source of messages to post to RabbitMQ
  # - Uses a CSV file as a source of expected messages to check
  # - uses common steps to control the bridge
  # All in 51 lines of code, including comments!

  Background:
    * print rabbit_bridge.url
    * url rabbit_bridge.url
    * callonce read('../helpers/schemas.feature')

  # Get the bridge to start listening using a common step from feature file
  Scenario: Start listening
    * call read('../helpers/bridge/bridge-start.feature')

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

  # Get the bridge to stop listening using a common step from feature file
  Scenario: Stop listening
    * call read('../helpers/bridge/bridge-stop.feature')

  Scenario Outline: Check results
    Given path 'message', "<queueName>", "<index>"
    When method get
    Then status 200
    And match response.id == parseInt("<id>")
    And match response.queueName == "<queueName>"
    And match response.message == "<message>"
    And match response == MESSAGE_SCHEMA

    Examples:
      | read('../examples/sample-messages-from-queues.csv') |

  # Reset the bridge using a common step from feature file
  Scenario: Reset the bridge
    * call read('../helpers/bridge/bridge-reset.feature')
