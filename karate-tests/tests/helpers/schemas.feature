Feature: Define all JSON schemas returned by the API

  # To make these schema variables available in a parent feature add the following to the Background of the parent:
  #
  #    * callonce read('classpath:helper/schemas.feature')
  #    * callonce read('../helper/schemas.feature')
  #
  # This will use shared scope meaning all variables def'd here will become available in the parent.
  #
  # To validate a single object against a schema:
  #    And match response == MESSAGE_SCHEMA
  #
  # To validate all objects in an array:
  #    And match each response == MESSAGE_SCHEMA

  Scenario: The API schemas
    # This is a very simple schema definition for a message, we could have checked the queueName is one of 'red', 'amber' or 'green'
    # Note that:
    # 1. a single # implies the field is required
    # 2. a double ## implies the field is optional
    * def MESSAGE_SCHEMA =
      """
      {
        "id": "#number",
        "queueName": "#string",
        "message": "#string"
      }
      """
