## Overview

This folder contains Karate tests that demonstrate how to use the Rabbit Bridge to test against expected messages that
should have been posted to RabbitMQ.

The example tests also provide a source of examples of how to use Karate to test REST APIs.

## Running the Tests

* Start the Rabbit Bridge application.
  ```shell
  # In a new command line window
  cd rabbit-bridge
  .\gradlew.bat bootRun
  ```
* Launch the `rabbit-play-local` Docker environment.
  ```shell
  # In a new command line window
  cd rabbit-play-local
  docker-compose up -d
  ```
* Run the tests:
  ```shell
  # In a new command line window
  cd karate-tests
  java -jar karate-1.5.1.jar tests/features
  ```

Karate will create a HTML report and display its location in the console output.
