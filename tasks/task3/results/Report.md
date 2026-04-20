# Report

## Архитектура

Реализована федеративная архитектура с использованием Apollo Federation.
Система состоит из следующих сабграфов:
- booking-subgraph
- hotel-subgraph
- promocode-subgraph
- apollo-gateway

## Booking Service

- Реализован ACL на уровне GraphQL resolver
- Пользователь может получать только свои бронирования
- Используется заголовок userid
- Добавлена имитация вызова внешнего сервиса (REST/gRPC)

## Hotel Service

- Реализован __resolveReference
- Решена проблема N+1 с помощью DataLoader
- Добавлен batching и кеширование

## Promocode Service

- Логика промокодов вынесена в отдельный сервис
- Использован @override для поля discountPercent
- Добавлено поле discountInfo с использованием @requires

## Gateway

- Агрегирует все сабграфы
- Прокидывает заголовок userid через RemoteGraphQLDataSource

## Результат

Система позволяет:
- агрегировать данные из разных доменов
- избегать N+1
- реализовать ACL
- гибко расширять API через GraphQL Federation