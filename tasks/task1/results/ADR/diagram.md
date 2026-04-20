# ADR: Пошаговая декомпозиция монолита Hotelio с использованием Strangler Fig

**Автор:** Митрофанов Роман  
**Дата:** 2026-04-13  

---

## Контекст

Hotelio - платформа бронирования отелей (~1 млн пользователей).  
Система реализована как **монолит**, все модули развертываются вместе и используют общую базу данных (Spring Boot + PostgreSQL).

С ростом нагрузки и команды возникли проблемы:
- высокая связность компонентов  
- невозможность масштабировать отдельные части  
- долгий time-to-market  
- высокий риск изменений  

Компания переходит к **микросервисной архитектуре**, система разбивается на независимые сервисы с собственной ответственностью и данными с постепенной миграцией.

В рамках данного ADR реализуется только **Booking-lite** сервис.
Остальные сервисы (User, Hotel, Promo, Review) показаны как целевое состояние и будут выделяться в рамках последующих этапов.

---

## Функциональные требования

| № | Actor | Use Case | Описание |
| :-: | :- | :- | :- |
| 1 | Пользователь | Поиск отелей | Получение списка |
| 2 | Пользователь | Бронирование | Создание брони |
| 3 | Пользователь | Промокоды | Применение скидки |
| 4 | Пользователь | Отзывы | Просмотр и создание |
| 5 | Система | Валидация | Проверка пользователя, отеля |

---

## Нефункциональные требования

| № | Требование |
| :-: | :- |
| 1 | Масштабируемость по доменам |
| 2 | Минимальный downtime |
| 3 | Независимые релизы |
| 4 | Наблюдаемость |
| 5 | Постепенная миграция |

---

## Ключевые проблемы

### 1. God Service (BookingService)

BookingService выполняет роль **оркестратора (orchestration)** , который управляет бизнес-процессом и вызывает другие сервисы:
- вызывает User, Hotel, Promo, Review  
- содержит всю бизнес-логику  

Это приводит к высокой связности и сложности изменений.

---

### 2. Общая база данных

Все сервисы используют одну БД:
- нет изоляции данных  
- высокая конкуренция за ресурсы  

Это нарушает принцип **bounded context** (границы бизнес-домена).

---

### 3. Синхронные вызовы

Сервисы вызывают друг друга напрямую:
- увеличивается latency  
- возникает риск **cascading failure** - ситуации, когда сбой одного компонента вызывает цепочку отказов  

---

## Решение

### Подход

- **Strangler Fig** - стратегия постепенной замены монолита новыми сервисами без остановки системы  
- выделение **bounded contexts** (Booking, User, Hotel, Promo, Review)  
- переход к **database per service** - у каждого сервиса своя база данных  
- использование **API Gateway** - единой точки входа, маршрутизирующей запросы  

---

## C4 - Контекст

```plantuml
@startuml C4_Context
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

Person(user, "User")
System_Ext(payment, "Payment Provider")

System_Boundary(system, "Hotelio") {
    System(api, "API Gateway", "Entry point")
    System(monolith, "Monolith", "Legacy")
}

Rel(user, api, "HTTPS")
Rel(api, monolith, "Routes")
Rel(monolith, payment, "Calls")

@enduml
```

---

Диаграмма **C4 - Контейнеры (target)**  отражает целевую архитектуру системы после завершения миграции. Текущий ADR реализует только первый шаг - выделение Booking-lite сервиса, отраженный на диаграмме **C4 - Переходная архитектура**.

## C4 - Контейнеры (target)

```plantuml
@startuml C4_Container
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

Person(user, "User")

System_Boundary(system, "Hotelio Microservices") {

    Container(api, "API Gateway")

    Container(booking, "Booking Service", "Orchestration")
    Container(user_s, "User Service")
    Container(hotel, "Hotel Service")
    Container(promo, "Promo Service")
    Container(review, "Review Service")

    Container(kafka, "Kafka", "Event-driven integration")

    ContainerDb(booking_db, "Booking DB")
    ContainerDb(user_db, "User DB")
    ContainerDb(hotel_db, "Hotel DB")
}

Rel(user, api, "Uses")

Rel(api, booking, "REST")
Rel(api, hotel, "REST")

Rel(booking, user_s, "Validate user")
Rel(booking, hotel, "Validate hotel")
Rel(booking, promo, "Apply promo")
Rel(booking, review, "Check rating")

Rel(booking, booking_db, "Writes")
Rel(user_s, user_db, "Reads")
Rel(hotel, hotel_db, "Reads")

Rel(booking, kafka, "Publishes events")
Rel(review, kafka, "Consumes events")

@enduml
```

---

## C4 - Переходная архитектура

```plantuml
@startuml C4_Container
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

Person(user, "User")

System_Boundary(system, "Hybrid") {

    Container(api, "API Gateway")
    Container(monolith, "Monolith")
    Container(booking, "Booking-lite Service")

    ContainerDb(monolith_db, "Monolith DB")
    ContainerDb(booking_db, "Booking DB")
}

Rel(user, api, "HTTPS")

Rel(api, monolith, "Legacy routes")
Rel(api, booking, "New routes")

Rel(monolith, monolith_db, "CRUD")
Rel(booking, booking_db, "Writes")

Rel(booking, monolith, "Internal API calls")

@enduml
```

---

## Выбор первого сервиса

### Booking-lite

Полный вынос Booking слишком рискован.  
Выбран **инкрементальное извлечение** - поэтапный вынос функциональности.

---

## План миграции (Strangler Fig)

Миграция выполняется поэтапно, без остановки системы. На каждом этапе система остаётся работоспособной.

---

### Этап 1: Введение API Gateway

**API Gateway** - единая точка входа, через которую проходят все запросы.

**Что делаем:**
- добавляем API Gateway  
- весь трафик направляется через него  

**Результат:**
- клиент больше не обращается напрямую к монолиту
- появляется контроль над трафиком

---

### Этап 2: Выделение Booking-lite

**Что делаем:**
- создаём новый сервис  
- переносим API бронирования  
- сервис вызывает монолит  

**Результат:**
- запросы на бронирование идут в новый сервис
- фактическая логика остаётся в монолите

---

### Этап 3: Подготовка данных

**Что делаем:**
- выделяем данные бронирований  
- настраиваем синхронизацию (например, брокер сообщений Kafka для event-driven взаимодействия, сервисы обмениваются событиями, а не только прямыми вызовами)  

**Результат:**
- Booking Service получает контроль над своими данными

---

### Этап 4: Переключение трафика

Используем **Feature flags** - механизм, позволяющий включать новую функциональность для части пользователей.

**Что делаем:**
- переводим часть пользователей (например, 5% - 20% - 100%) на новый сервис 

**Результат:**
- постепенный переход без резких изменений

---

### Этап 5: Частичный перенос бизнес-логики

В рамках данного ADR выполняется начальный перенос логики.

Полный перенос всех зависимостей (Promo, Review и др.) выполняется на следующих этапах миграции.

---

### Этап 6: Завершение миграции (вне рамок текущего ADR)

Полное удаление логики из монолита выполняется после завершения декомпозиции всех зависимых сервисов.

---

## Ключевые принципы миграции

- система остаётся работоспособной  
- изменения происходят постепенно  
- возможен откат  

---

## Риски

- **eventual consistency** - данные могут временно расходиться между сервисами  
- рост сложности системы  
- увеличение сетевых задержек  

---

## Меры по снижению рисков

- **Feature flags** - включаем новую функциональность постепенно (например, для 5% пользователей)
- **Fallback на монолит** - если новый сервис работает нестабильно, система автоматически возвращается к старой логике
- **Идемпотентность** - повторный запрос не приводит к созданию дублирующих бронирований
- **Мониторинг и алерты** - быстро обнаруживаем ошибки и реагируем на них

---

## Итог

Постепенная миграция позволяет:
- снизить риски  
- сохранить стабильность  
- перейти к масштабируемой архитектуре  
