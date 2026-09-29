# FitnessAPK — заметки для ассистента

- Android-приложение (Kotlin, Compose, Room), один модуль `app`. Проектирование — `docs/DESIGN.md`.
- Версии зависимостей — только в `gradle/libs.versions.toml`.
- UI-тексты на русском.
- Схема Room экспортируется в `app/schemas/`; при изменении сущностей повышать версию БД и добавлять миграцию — данные пользователя терять нельзя.
- Сборка и проверка — GitHub Actions (`.github/workflows/android.yml`): `testDebugUnitTest`, `assembleDebug`, `assembleRelease`.
- Расчёты (1ПМ, тоннаж, точки прогресса) — в `data/Stats.kt`, покрыты тестами в `app/src/test`.
