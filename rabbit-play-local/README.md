## Rabbit MQ

```shell
# To start the RabbitMQ environment
cd rabbit-play-local
docker compose up -d

# To stop and destroy the containers
docker compose down -v
```

Management UI: http://localhost:15672/

```text
Username: guest
Password: guest

Username: mvm
Password: Password123!

Virtual Host: mvm
Exchange: primary
Exchange: mvm
Queues: red, amber, green
```
