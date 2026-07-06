MilitaryManagement – клиент-серверное приложение для автоматизированного учёта военнослужащих с разделением на категории (рядовой состав, командование, контрактники, награждённые). Разработано на Java с использованием JavaFX для клиента и SQLite для хранения данных.

Возможности:
- Аутентификация пользователей (bcrypt)
- Просмотр, добавление, редактирование, удаление записей
- Фильтрация по категориям и полнотекстовый поиск
- Блокировка записей при одновременном редактировании
- Импорт из JSON и экспорт в CSV
- Детальное логирование действий (аудит)
- Многопоточный сервер

Технологии:
Java 25
JavaFX 17
Maven
SQLite
Gson (JSON)
jBCrypt

Сборка и запуск для разработчика:
- Установите JDK 25 и Maven
- Клонируйте репозиторий
- Замените localhost в ServerConnector.java и Protocol.java на ваш серверный IP
- Выполните mvn clean compile assembly:single
- Запустите сервер командой
java -cp target/military-management-1.0-SNAPSHOT-jar-with-dependencies.jar com.example.military.server.ServerMain
- Запустите клиент:
java -cp target/military-management-1.0-SNAPSHOT-jar-with-dependencies.jar com.example.military.client.FXClientMain
- Введите полученные от администратора данные для логина.

Сборка и запуск для пользователя:
- Запросите установщик программы у вашего администратора
- Запустите установщик, измените настройки установки, если это требуется
- Запустите программу нажатием по иконке

Структура проекта
src/ – исходный код.
com.example.military.model – модели данных
com.example.military.shared – общие утилиты (протокол, JSON)
com.example.military.server – серверная логика
com.example.military.client – клиентский интерфейс
com.example.military.service – сервисы (сортировка, файлы)
resources/ – CSS, иконки
pom.xml – конфигурация Maven
