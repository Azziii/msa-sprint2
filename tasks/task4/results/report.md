# Отчёт по заданию 4: Автоматизация развёртывания и тестирования

## Общая цель

В рамках задания была реализована автоматизация сборки, тестирования и деплоя сервиса `booking-service` с использованием Docker/Podman, Helm и Minikube.

Цель - ускорить доставку изменений, повысить стабильность деплоя и обеспечить воспроизводимость окружения.

---

## Реализация сервиса

Был реализован простой HTTP-сервис на Go со следующими endpoint:

* `GET /ping` - основной endpoint для проверки сервиса
* `GET /health` - проверка состояния
* `GET /ready` - проверка готовности

Также добавлена поддержка фича-флага:

* переменная окружения `ENABLE_FEATURE_X`
* при включении меняется поведение `/ping` (возвращает `pong-feature`)

---

## Контейнеризация

Сервис упакован в Docker-образ.

Сборка выполняется командой:

```bash
docker build -t booking-service:latest ./booking-service
```

Для работы с Minikube использовался подход:

```bash
podman save -o booking-service.tar booking-service:latest
minikube image load booking-service.tar
```

---

## Helm Chart

Использован и доработан Helm chart из репозитория.

### Основные настройки:

* Deployment:

  * `replicaCount` управляется через values
  * пробрасываются переменные окружения (`env`)
  * заданы `livenessProbe` и `readinessProbe` на `/ping`
  * добавлены `resources` (requests/limits)

* Service:

  * тип `ClusterIP`
  * порт `80 - 8080`

---

## Конфигурации окружений

Созданы два файла:

### values-staging.yaml

* `replicaCount: 1`
* `ENABLE_FEATURE_X = true`
* минимальные ресурсы

### values-prod.yaml

* `replicaCount: 3`
* `ENABLE_FEATURE_X = false`
* увеличенные ресурсы

---

## Деплой в Kubernetes

Деплой выполнялся через Helm:

```bash
helm upgrade --install booking-service ./helm/booking-service
```

Проверка:

```bash
kubectl get pods
kubectl get svc
```

---

## Проверка работоспособности

### 1. Проверка статуса

```bash
./check-status
```

---

### 2. Проверка DNS (Service Discovery)

```bash
./check-dns.sh
```

Результат:

```
pong
[PASS] DNS test succeeded
```

---

### 3. Проверка через port-forward

```bash
kubectl port-forward svc/booking-service 8080:80
curl http://localhost:8080/ping
```

---

## CI/CD Pipeline

Реализован `.gitlab-ci.yml` со стадиями:

* `build`
* `test`
* `deploy`
* `tag`

### Особенности:

* build - сборка образа (логическая стадия)
* test - проверка endpoint `/ping`
* deploy - деплой через Helm
* tag - создание git-тега

Pipeline адаптирован для локального запуска через `gitlab-ci-local`.

---

## Service Discovery

Сервис доступен внутри кластера по DNS-имени:

```
http://booking-service/ping
```

Это подтверждено через `check-dns.sh`.

---

## Особенности и решения

* Использовался Windows + Minikube - применён Git Bash для запуска скриптов
* Для загрузки образа использован `podman save + minikube image load`
* `imagePullPolicy: Never` для использования локального образа
* Port-forward используется для доступа извне

---

## Итог

В результате:

* сервис контейнеризирован
* реализован Helm-деплой
* настроены staging/prod конфигурации
* реализован CI/CD pipeline
* настроен Service Discovery
* проведены проверки работоспособности

Система готова к дальнейшему развитию и масштабированию.

---
