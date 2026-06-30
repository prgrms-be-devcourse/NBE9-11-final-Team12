# Grafana Cloud 운영 모니터링

## 요약

운영 서버에는 무거운 Prometheus와 Grafana를 띄우지 않는다.
Alloy가 Docker 내부망에서 백엔드 actuator와 exporter 메트릭을 수집하고 Grafana Cloud로 전송한다.

```text
backend / exporter
→ Alloy
→ Grafana Cloud Prometheus / Loki
```

외부에는 `/actuator/health`만 공개하고 `/actuator/prometheus`는 nginx에서 차단한다.
Alloy는 nginx를 거치지 않고 Docker 내부망으로 직접 `/actuator/prometheus`를 수집한다.

## 구성 파일

| 파일 | 역할 |
| --- | --- |
| `infra/compose/compose.grafana-cloud.yaml` | Alloy, node-exporter, redis-exporter, mysqld-exporter 실행 |
| `infra/compose/alloy/config.cloud.alloy` | 단일 backend 수집 설정 |
| `infra/compose/alloy/config.cloud-bluegreen.alloy` | blue/green backend 수집 설정 |
| `infra/compose/.env.grafana-cloud.example` | 운영 `.env` 예시 |

## 운영 서버 env

운영 서버의 실제 `.env`에 아래 값을 채운다.
이 파일은 Git에 커밋하지 않는다.

```env
GRAFANA_CLOUD_PROM_URL=...
GRAFANA_CLOUD_PROM_USER=...
GRAFANA_CLOUD_API_KEY=glc_...
GRAFANA_CLOUD_LOKI_URL=...
GRAFANA_CLOUD_LOKI_USER=...
MYSQL_EXPORTER_USER=exporter
MYSQL_EXPORTER_PASSWORD=...
```

Grafana Cloud 화면에서 값은 다음처럼 매핑한다.

| Grafana Cloud 값 | 프로젝트 env |
| --- | --- |
| `GCLOUD_HOSTED_METRICS_URL` | `GRAFANA_CLOUD_PROM_URL` |
| `GCLOUD_HOSTED_METRICS_ID` | `GRAFANA_CLOUD_PROM_USER` |
| `GCLOUD_RW_API_KEY` | `GRAFANA_CLOUD_API_KEY` |
| `GCLOUD_HOSTED_LOGS_URL` | `GRAFANA_CLOUD_LOKI_URL` |
| `GCLOUD_HOSTED_LOGS_ID` | `GRAFANA_CLOUD_LOKI_USER` |

## mysqld-exporter 계정

MySQL에 exporter 전용 계정을 최초 1회 생성한다.

```sql
CREATE USER 'exporter'@'%' IDENTIFIED BY '<MYSQL_EXPORTER_PASSWORD>' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
```

## 실행

먼저 운영 서버에서 앱 컨테이너가 붙어 있는 Docker network 이름을 확인한다.

```bash
docker network ls
```

`.env.grafana-cloud`의 `APP_DOCKER_NETWORK`에 해당 network 이름을 넣는다.
예를 들어 운영 compose project 이름이 `sisibibi`이면 보통 `sisibibi_default`가 된다.

단일 backend 환경:

```bash
cd infra/compose
cp .env.grafana-cloud.example .env.grafana-cloud
# .env.grafana-cloud 값 수정

docker compose --env-file .env.grafana-cloud \
  -f compose.grafana-cloud.yaml \
  up -d alloy node-exporter redis-exporter mysqld-exporter
```

blue/green 환경:

```env
ALLOY_CONFIG_FILE=config.cloud-bluegreen.alloy
BACKEND_BLUE_TARGET=backend-blue:8080
BACKEND_GREEN_TARGET=backend-green:8080
```

실제 blue/green compose 파일이 있다면 함께 지정한다.

```bash
docker compose --env-file .env.grafana-cloud \
  -f compose.grafana-cloud.yaml \
  up -d alloy node-exporter redis-exporter mysqld-exporter
```

## 확인

Alloy 로그:

```bash
cd infra/compose
docker compose --env-file .env.grafana-cloud \
  -f compose.grafana-cloud.yaml logs -f alloy
```

Grafana Cloud Explore에서 확인할 PromQL:

```promql
up
jvm_memory_used_bytes
http_server_requests_seconds_count
hikaricp_connections_active
redis_up
mysql_up
node_memory_MemAvailable_bytes
```

Loki 로그 확인:

```logql
{job="sisibibi-api"}
```

## nginx 정책

외부 사용자에게 prometheus endpoint를 공개하지 않는다.

```nginx
location = /actuator/health {
    proxy_pass http://backend_upstream;
}

location /actuator/prometheus {
    return 403;
}
```

## 주의사항

- `GRAFANA_CLOUD_API_KEY`는 Git, Notion, PR, 채팅에 올리지 않는다.
- 스크린샷에 노출된 `glc_` 토큰은 폐기하고 새로 발급한다.
- 운영 서버에서 Prometheus/Grafana 컨테이너를 함께 띄우지 않는다.
- Alloy가 죽으면 Grafana Cloud로 지표가 가지 않으므로 외부 uptime check는 별도로 유지한다.
