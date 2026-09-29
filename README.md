# FitnessAPK

Личное Android-приложение — дневник силовых тренировок: подходы, шаблоны, таймер отдыха, история, графики прогресса и вес тела. Работает офлайн, данные хранятся на телефоне.

Проектирование (цели, задачи, функционал, экраны, визуал, архитектура, дорожная карта) — в [docs/DESIGN.md](docs/DESIGN.md).

## Как получить APK

1. Откройте вкладку **Actions** репозитория → последний успешный запуск **Android CI**.
2. Скачайте артефакт **fitness-apk** (zip) и распакуйте.
3. Установите на телефон:
   - `app-debug.apk` — отладочная сборка (ставится как отдельное приложение «Fitness» с id `…fitness.debug`);
   - `app-release.apk` — оптимизированная сборка, её и стоит использовать постоянно.
4. На телефоне разрешите установку из неизвестных источников для файлового менеджера/браузера.

Для версии с релизом: создайте тег `v0.1.0` и отправьте его (`git tag v0.1.0 && git push origin v0.1.0`) — CI опубликует GitHub Release с APK.

## Подпись

По умолчанию обе сборки подписываются общим ключом `app/debug.keystore` из репозитория, поэтому новые APK из CI устанавливаются поверх старых без потери данных.

Собственный ключ для release (необязательно, делается один раз **до** начала реального использования — смена ключа требует переустановки с потерей данных):

```bash
keytool -genkeypair -v -keystore release.jks -alias fitness -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # результат — в секрет FITNESS_KEYSTORE_BASE64
```

Секреты репозитория (Settings → Secrets and variables → Actions): `FITNESS_KEYSTORE_BASE64`, `FITNESS_KEYSTORE_PASSWORD`, `FITNESS_KEY_ALIAS`, `FITNESS_KEY_PASSWORD`.

## Локальная сборка

Нужны JDK 17+ и Android SDK (Android Studio). 

```bash
./gradlew testDebugUnitTest   # юнит-тесты
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```

## Стек

Kotlin · Jetpack Compose (Material 3) · Room · Navigation Compose · Coroutines/Flow · minSdk 26 / targetSdk 35.

## Структура

```
app/src/main/java/com/bugsjkeeee/fitness/
├── FitnessApp.kt            # Application, контейнер зависимостей
├── MainActivity.kt
├── data/                    # Room: сущности, DAO, база, репозиторий, расчёты
└── ui/
    ├── Navigation.kt        # маршруты и нижняя навигация
    ├── components/          # общие компоненты, график, форматирование
    ├── screens/             # экраны и их ViewModel
    └── theme/
```
