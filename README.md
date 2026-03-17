MilitaryManagement
MilitaryManagement – клиент-серверное приложение для автоматизированного учёта военнослужащих с разделением на категории (рядовой состав, командование, контрактники, награждённые). Разработано на Java с использованием JavaFX для клиента и SQLite для хранения данных.

Возможности
Аутентификация пользователей (bcrypt).

Просмотр, добавление, редактирование, удаление записей.

Фильтрация по категориям и полнотекстовый поиск.

Блокировка записей при одновременном редактировании.

Импорт из JSON и экспорт в CSV.

Детальное логирование действий (аудит).

Многопоточный сервер.

Технологии
Java 25

JavaFX 17

Maven

SQLite

Gson (JSON)

jBCrypt

Сборка и запуск
Установите JDK 25 и Maven.

Клонируйте репозиторий.

Выполните mvn clean compile assembly:single.

Запустите сервер:
java -cp target/military-management-1.0-SNAPSHOT-jar-with-dependencies.jar com.example.military.server.ServerMain

Запустите клиент:
java -cp target/military-management-1.0-SNAPSHOT-jar-with-dependencies.jar com.example.military.client.FXClientMain

Учётные записи по умолчанию:

user001@arm.ru / kD9#mP2$rT5@

user002@arm.ru / xL7&nR4$wQ9*

Структура проекта
src/ – исходный код.

com.example.military.model – модели данных.

com.example.military.shared – общие утилиты (протокол, JSON).

com.example.military.server – серверная логика.

com.example.military.client – клиентский интерфейс.

com.example.military.service – сервисы (сортировка, файлы).

resources/ – CSS, иконки.

pom.xml – конфигурация Maven.
