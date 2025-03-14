Feature: Finally, use common steps to control the bridge

  Background:
    * url rabbit_bridge.url

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
