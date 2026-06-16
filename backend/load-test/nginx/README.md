# Nginx Stage Load Balancer

## Purpose

Use this local Nginx container to test the speaking queue through a production-like load-balancer shape.

Flow:

```text
k6 -> Nginx:8088 -> Spring Boot 8080 / 8081 -> MySQL
```

## Prerequisites

Run two Spring Boot instances with the same MySQL database.

```text
--spring.profiles.active=load-test --server.port=8080
--spring.profiles.active=load-test --server.port=8081
```

## Start

Run from this directory.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/nginx

MSYS_NO_PATHCONV=1 docker run -d --name sisibibi-stage-lb \
  --network grafana_default \
  --ulimit nofile=65535:65535 \
  -p 8088:8088 \
  -v "$(pwd -W)/nginx.conf:/etc/nginx/nginx.conf:ro" \
  nginx:1.27-alpine
```

## Check

```bash
curl http://localhost:8088/
```

From another container in the Grafana network:

```bash
docker run --rm --network grafana_default curlimages/curl:latest -i http://sisibibi-stage-lb:8088/
```

If Spring Security returns `403`, the load balancer is still reachable and proxying correctly.
The important part is that the response comes from Spring Boot, not the default Nginx welcome page.

To inspect the active Nginx config:

```bash
docker exec -it sisibibi-stage-lb nginx -T
```

## Stop

```bash
docker rm -f sisibibi-stage-lb
```
