# Hotel Booking System

## Состав системы

Проект реализует декомпозицию монолита на микросервисы:

* **hotelio-monolith** - REST API (входная точка)
* **booking-service** - gRPC сервис бронирований
* **booking-history-service** - асинхронная обработка (Kafka)
* **PostgreSQL** - отдельные базы для сервисов
* **Kafka + Zookeeper** - событийная шина

---

## Запуск

```bash
podman-compose up --build
```

Доступ:

* REST API: http://localhost:8084
* gRPC: localhost:9090
* Kafka: localhost:9092

---

## Основные сценарии

### Создание бронирования

```bash
curl -X POST http://localhost:8084/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user-3","hotelId":"test-hotel-1","promoCode":""}'
```

---

### Получение списка бронирований

```bash
curl "http://localhost:8084/api/bookings?userId=test-user-3"
```

---

### Прямой вызов gRPC

```bash
grpcurl -plaintext -d '{"userId":"test-user-3"}' \
  localhost:9090 booking.BookingService/ListBookings
```

---

## Как работает система

### Создание бронирования

```text
REST (monolith)
 → gRPC (booking-service)
 → PostgreSQL (booking-db)
 → Kafka (booking.created)
 → booking-history-service
 → PostgreSQL (booking-history-db)
```

---

### Получение списка бронирований

```text
REST (monolith)
 → gRPC (booking-service)
 → PostgreSQL (booking-db)
```

---

## Стратегия миграции

Использован подход постепенного вытеснения (Strangler pattern):

1. Выделен новый сервис `booking-service` со своей БД
2. Монолит перестаёт писать напрямую в таблицу `booking`
3. Все новые операции записи идут через gRPC
4. Чтение также переключено на микросервис

### Работа с данными

* Новые бронирования сохраняются только в `booking-service`
* Исторические данные фиксируются через Kafka
* Старые данные из монолита **не мигрируются** (упрощение задания)

---

## To-Be стратегия

После миграции система работает следующим образом:

* **booking-service - источник истины**

  * полностью владеет данными бронирований
  * монолит не использует свою БД для booking

* **монолит - orchestration слой**

  * принимает REST-запросы
  * делегирует операции в gRPC сервис

* **асинхронная архитектура**

  * события публикуются в Kafka (`booking.created`)
  * обработка истории не влияет на основной поток

* **Database per Service**

  * у каждого сервиса своя БД
  * отсутствует shared database

* **расширяемость**

  * можно добавлять новые consumer-сервисы
  * без изменений в существующих сервисах

---

## Роли сервисов

| Сервис                  | Ответственность                      |
| ----------------------- | ------------------------------------ |
| monolith                | REST API, orchestration              |
| booking-service         | бизнес-логика, хранение бронирований |
| booking-history-service | асинхронная обработка и история      |

---

## Ограничения

* нет миграции старых данных
* нет retry / DLQ для Kafka
* нет версионирования событий
* упрощённая обработка ошибок

---

## Итог

Система переведена на микросервисную архитектуру:

* запись и чтение вынесены в отдельный сервис
* добавлена событийная модель через Kafka
* монолит выполняет роль API gateway

Архитектура готова к масштабированию и дальнейшей декомпозиции.
