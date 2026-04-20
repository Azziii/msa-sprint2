# Отчёт по заданию 5: Управление трафиком с Istio

## Цель

Целью задания была настройка управления трафиком для сервиса `booking-service` с использованием **Istio Service Mesh**:

* канареечный релиз (canary)
* fallback при ошибках
* retry и circuit breaking
* маршрутизация по feature flag

---

## Установка Istio

Istio был установлен в кластер Minikube:

```bash
istioctl install --set profile=demo -y
kubectl label namespace default istio-injection=enabled --overwrite
```

Проверка:

```bash
kubectl get pods -n istio-system
```

Все компоненты (`istiod`, `ingressgateway`) находятся в состоянии `Running`.

---

## Две версии сервиса

Развернуты две версии сервиса через Helm:

* **v1** - основная версия
* **v2** - версия с включённым feature flag

### values-v1.yaml

* `version: v1`
* `ENABLE_FEATURE_X = false`
* `replicaCount: 2`

### values-v2.yaml

* `version: v2`
* `ENABLE_FEATURE_X = true`
* `replicaCount: 1`

Ключевой момент - добавление label:

```yaml
version: v1 / v2
```

---

## Деплой

```bash
helm upgrade --install booking-v1 ./helm/booking-service -f values-v1.yaml
helm upgrade --install booking-v2 ./helm/booking-service -f values-v2.yaml
```

Проверка:

```bash
kubectl get pods --show-labels
```

---

## Настройка Istio

### 1. Gateway

Создан Gateway для приёма входящего HTTP-трафика:

```yaml
kind: Gateway
metadata:
  name: booking-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 80
        protocol: HTTP
      hosts:
        - "*"
```

---

### 2. VirtualService

Реализованы:

* feature flag routing
* canary deployment (90/10)
* retry

```yaml
http:

# Feature flag
- match:
    - headers:
        X-Feature-Enabled:
          exact: "true"
  route:
    - destination:
        host: booking-service
        subset: v2

# Canary
- route:
    - destination:
        host: booking-service
        subset: v1
      weight: 90
    - destination:
        host: booking-service
        subset: v2
      weight: 10

  retries:
    attempts: 3
    perTryTimeout: 2s
```

---

### 3. DestinationRule

Настроены:

* circuit breaking
* outlier detection

```yaml
trafficPolicy:
  connectionPool:
    http:
      http1MaxPendingRequests: 1
  outlierDetection:
    consecutive5xxErrors: 1
    interval: 5s
    baseEjectionTime: 10s

subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

---

### 4. EnvoyFilter

Добавлен фильтр для обработки feature flag:

* анализирует заголовок `X-Feature-Enabled`
* направляет трафик в v2

---

## 🔍 Особенности реализации

### 1. Разные порты в Istio

Ingress gateway слушает порт **8080**, а не 80:

```bash
netstat -tln
→ 0.0.0.0:8080 LISTEN
```

Поэтому для проверки использовалось:

```bash
kubectl port-forward pod/<ingress-pod> -n istio-system 9090:8080
```

---

### 2. Проблема доступа через NodePort

В Windows среде Minikube IP (`192.168.x.x`) оказался недоступен напрямую.
Решение - использование `port-forward`.

---

### 3. Обязательное наличие Gateway

Без Gateway ingress не принимает трафик → ошибка `connection refused`.

---

## Проверка

### 1. Istio

```bash
./check-istio.sh
```

---

### 2. Canary deployment

```bash
./check-canary.sh
```

Результат: ~90% трафика идёт в v1, ~10% в v2.

---

### 3. Feature flag

```bash
./check-feature-flag.sh
```

Результат: запрос направляется в v2.

---

### 4. Fallback

При отключении pod v1:

```bash
kubectl delete pod -l version=v1
./check-fallback.sh
```

Результат: трафик автоматически идёт в v2.

---

## ✅ Итог

В результате:

* настроен Istio Service Mesh
* реализован canary deployment (90/10)
* реализована маршрутизация по feature flag
* настроены retry и circuit breaking
* реализован fallback при ошибках
* обеспечена работа ingress через Istio Gateway

Система готова к управлению трафиком и безопасному выкатыванию новых версий.

---
