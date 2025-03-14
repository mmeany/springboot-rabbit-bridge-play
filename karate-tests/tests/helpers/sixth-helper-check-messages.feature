Feature: Finally, use common steps to control the bridge

  Background:
    * url rabbit_bridge.url
    * callonce read('../helpers/schemas.feature')

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
