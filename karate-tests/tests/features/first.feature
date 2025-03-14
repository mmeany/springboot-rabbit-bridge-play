Feature: Lets see how this bridge thing works

  Background:
    # The following line is not required, it has been included to show that karate-config.js has been loaded
    * print rabbit_bridge.url
    # This is required, it sets the base URL for all future `path` calls
    * url rabbit_bridge.url

  Scenario: As a test engineer I can access messages posted to Rabbit from the bridge API
    # Start listening to the queues, there are a few points to note here:
    # 1. The path is set to 'start-listening' which will be appended to the base URL set in the Background
    # 2. The request body is a JSON object with a single key 'queueNames' which is an array of the queues to listen to
    # 3. The method is set to 'post'
    # 4. The expected status code is 200
    Given path 'start-listening'
    And request
      """
      {
        "queueNames": ["red", "amber", "green"]
      }
      """
    When method post
    Then status 200

    # Provided the above step was successful, we can now send messages to the queues
    # These requests follow the same pattern as the last
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

    # Okay, all messages have been sent, lets stop listening to the queues which will allow the bridge to be queried

    Given path 'stop-listening'
    When method post
    Then status 200

    # Here is the first test, we will query the bridge for the first message in the 'red' queue
    # The path is followed by three parameters:
    # First is the endpoint to call
    # Second is the queue name
    # Third is the index of the message in the queue
    # Karate will use these parameters to build the URL: rabbit_bridge.url + '/message/red/0'
    Given path 'message', "red", "0"
    When method get
    Then status 200
    # The following line is not required, it has been included to show the response
    And print response
    # Here we use Karate's match keyword to compare a field from the response to the an expected value
    And match response.message == "Hello, World!"

    Given path 'message', "red", "1"
    When method get
    Then status 200
    And match response.message == "Nice to see you again old chap"

    # We done testing this feature, reset the bridge to clear all messages it has captured and listeners it created
    Given path 'reset'
    When method post
    Then status 200
