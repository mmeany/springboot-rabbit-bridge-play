Feature: And now use an example to post a ton of messages from a CSV file

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

  # The Outline keyword is used to run the same scenario multiple times with different data from the Examples table
  # The table is read from a CSV file, 1st row is header, rest are data rows
  # Names from the header row are used as variables in the scenario; in this case <queueName> and <message>
  # This scenario will run once for each row in the CSV file
  Scenario Outline: Post messages - messages come from a CSV file
    Given path 'send'
    # Notice use of variables <queueName> and <message> from the Examples table
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

    Given path 'message', "red", "0"
    When method get
    Then status 200
    And print response
    And match response.message == "Hello, red!"
    And match response == MESSAGE_SCHEMA

    Given path 'message', "red", "1"
    When method get
    Then status 200
    And match response.message == "Nice to see you again old chap"
    # We can use the schema for MESSAGE_SCHEMA to validate the response
    And match response == MESSAGE_SCHEMA

    Given path 'message', "red", "4"
    When method get
    Then status 200
    And match response.message == "Do I have to stop?"
    # We can use the schema for MESSAGE_SCHEMA to validate the response
    And match response == MESSAGE_SCHEMA

    Given path 'reset'
    When method post
    Then status 200