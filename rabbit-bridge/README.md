## Playing with Rabbit MQ

An example of accessing Rabbit from a SpringBoot application.

A Rabbit environment is provided in the `rabbit-play-local` directory and instructions are provided in
[README.md](../rabbit-play-local/README.md).

The SpringBoot application presents a REST API to send messages to Rabbit and to capture messages from Rabbit that can
then be queried using the same REST API.

The REST API is documented using Swagger and can be accessed at [OpenAPI Docs](http://localhost:8080/swagger-ui.html).

To change the RabbitMQ environment, update the `application.yml` file in the `src/main/resources` directory.

## Using the API

There are two phases to using the API, capture and query.

### Capture

* Start listening for messages from RabbitMQ
    * `POST http://localhost:8080/start-listening`, pass in a list of queues to listen to which in this case are `red`,
      `amber`, and `green`.
    ```json
    {
      "queueNames": ["red", "amber", "green"]
    }
    ```
* Post messages to the queues, send a few of them.
    * `POST http://localhost:8080/send`, pass in the queue name and the message.
    ```json
    {
      "queueName": "red",
      "message": "Hello, red!"
    }
    ```
* Stop listening for messages from RabbitMQ
    * `POST http://localhost:8080/stop-listening`

### Query

* To get counts of messages received on each queue
    * `GET http://localhost:8080/count`
* To get a message from one of the queues
    * `GET http://localhost:8080/message/{queueName}/{index}`, indexing starts at 0.
    * For example, `GET http://localhost:8080/message/red/0` will return the first message from the `red` queue.
* To get a list of messages from a queue that match a [JSON Path](https://github.com/json-path/JsonPath)
    * `POST http://localhost:8080/search-by-path`, and in the body of the request:
    ```json
    {
      "queueName": "red",
      "jsonPath": "$[?(@.message == 'Hello, red!')]"
    }
    ```
    * For example, `GET http://localhost:8080/messages/red?jsonPath=$[?(@.message == 'Hello, red!')]` will return
      all messages from the `red` queue that match the message `Hello, red!`.

### Dispose

Once you are done, the listeners should be disposed of. This will also clear down the cached messages.

* `POST http://localhost:8080/reset`

## Example by CURL

On Windows, execute this in a Git Bash shell.

```shell
# Start listening
curl -s -X POST "http://localhost:8080/start-listening" -H "Content-Type: application/json" -d '{"queueNames":["red","amber","green"]}'

# Send a message
curl -s -X POST "http://localhost:8080/send" -H "Content-Type: application/json" -d '{"queueName":"red","message":"Hello, red!"}'
curl -s -X POST "http://localhost:8080/send" -H "Content-Type: application/json" -d '{"queueName":"red","message":"Ready..."}'
curl -s -X POST "http://localhost:8080/send" -H "Content-Type: application/json" -d '{"queueName":"red","message":"How are you"}'
curl -s -X POST "http://localhost:8080/send" -H "Content-Type: application/json" -d '{"queueName":"amber","message":"Steady..."}'
curl -s -X POST "http://localhost:8080/send" -H "Content-Type: application/json" -d '{"queueName":"red","message":"Do I have to stop?"}'
curl -s -X POST "http://localhost:8080/send" -H "Content-Type: application/json" -d '{"queueName":"green","message":"Go!"}'
curl -s -X POST "http://localhost:8080/send" -H "Content-Type: application/json" -d '{"queueName":"red","message":"Hello, red!"}'

# Stop listening
curl -s -X POST "http://localhost:8080/stop-listening"

# Get counts
curl -s -X GET "http://localhost:8080/count" | jq .

# Get a message
curl -s -X GET "http://localhost:8080/message/red/0" | jq .

# Search by path
curl -s -X POST "http://localhost:8080/search-by-path" -H "Content-Type: application/json" -d '{"queueName":"red","jsonPath":"$[?(@.message == '\''Hello, red!'\'')]"}' | jq .

declare -i I
declare -i COUNT
I=0
COUNT=$(curl -s -X GET "http://localhost:8080/count/red" | jq '.count')
while (( I < COUNT )); do
  echo "Fetching message ${I}:"
  curl -s -X GET "http://localhost:8080/message/red/${I}" | jq .
  (( I++ ))
done

# Reset
curl -s -X POST "http://localhost:8080/reset"
```
