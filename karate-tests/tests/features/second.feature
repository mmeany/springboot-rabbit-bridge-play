Feature: Okay, let'c check schema validation

  Background:
    * print rabbit_bridge.url
    * url rabbit_bridge.url
    # Pull in all schema definitions from the schemas.feature file, these can be used to validate responses
    * callonce read('../helpers/schemas.feature')

  Scenario: As a test engineer I can access messages posted to Rabbit from the bridge API
    Given path 'start-listening'
    And request
      """
      {
        "queueNames": ["red", "amber", "green"]
      }
      """
    When method post
    Then status 200

    Given path 'send'
    And request
      """
      {
        "queueName": "red",
        "message": "Hello, World!"
      }
      """
    When method post
    Then status 200

    Given path 'send'
    And request
      """
      {
        "queueName": "red",
        "message": "Nice to see you again old chap"
      }
      """
    When method post
    Then status 200

    Given path 'stop-listening'
    When method post
    Then status 200

    Given path 'message', "red", "0"
    When method get
    Then status 200
    And print response
    And match response.message == "Hello, World!"
    # We can use the schema for MESSAGE_SCHEMA to validate the response
    And match response == MESSAGE_SCHEMA

    Given path 'message', "red", "1"
    When method get
    Then status 200
    And match response.message == "Nice to see you again old chap"
    # We can use the schema for MESSAGE_SCHEMA to validate the response
    And match response == MESSAGE_SCHEMA

    Given path 'reset'
    When method post
    Then status 200
