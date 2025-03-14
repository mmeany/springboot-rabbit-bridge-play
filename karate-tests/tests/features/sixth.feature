Feature: Move everything into feature files

  # This would be more correct as there is a single scenario and no dependencies

  Background:
    * print rabbit_bridge.url
    * url rabbit_bridge.url

  # Get the bridge to start listening using a common step from feature files
  Scenario: Start listening
    * call read('../helpers/bridge/bridge-start.feature')
    * call read('../helpers/sixth-helper-send-messages.feature')
    * call read('../helpers/bridge/bridge-stop.feature')
    * call read('../helpers/sixth-helper-check-messages.feature')
    * call read('../helpers/bridge/bridge-reset.feature')
