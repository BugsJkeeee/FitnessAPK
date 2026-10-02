# Tempo — заметки для ассистента

- Требования — `docs/TZ_Tempo_в2.docx`; работа идёт по этапам из раздела 15 ТЗ. Не добавлять функции сверх ТЗ без согласования.
- Android, Kotlin + Compose, один модуль `app`, пакет `com.bugsjkeeee.tempo`. Версии зависимостей — только в `gradle/libs.versions.toml`.
- UI-тексты на русском.
- Логика таймера (`timer/TimerModel.kt`, `timer/TimerRun.kt`) не зависит от Android и покрыта тестами в `app/src/test`.
- Локально Android SDK недоступен: сборка и тесты проверяются в GitHub Actions.
- Звуки генерирует `tools/generate_sounds.py` в `app/src/main/res/raw`.
- Данные пользователя (Room, DataStore) должны переживать обновления: при изменении схемы БД — повышать версию и писать Migration, никаких destructive-миграций. id встроенных тренировок (f###, s###, c###) не менять и не перенумеровывать — новые добавлять в конец списков.
