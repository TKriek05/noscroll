# ScrollLess

Android-app die Instagram- en YouTube-gebruik zichtbaar maakt en afremt: tracking,
een bewust moment bij elke app-opening, en een streak om je eraan te houden.

Alles blijft lokaal op het toestel. Er is geen backend, geen account en geen
netwerkverkeer.

## Wat er werkt (MVP, fase 1 t/m 3)

- **Tracking** — schermtijd per dag voor Instagram en YouTube via `UsageStatsManager`,
  elke 15 minuten weggeschreven door een `WorkManager`-taak.
- **Interventie** — een `AccessibilityService` merkt op dat je een van de twee apps
  opent en zet er een wachtscherm overheen met een concrete vervanger-suggestie.
- **Beloning** — elke keuze wordt gelogd; een dag is "goed" als je niet vaker dan je
  eigen limiet alsnog doorging. Daaruit volgen een streak en punten.

Bewust nog niet: backend-sync, webdashboard, Home Assistant, andere apps dan IG/YT.

## Bouwen

Vereist Android Studio (Ladybug of nieuwer) met Android SDK 35.

```bash
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew installDebug         # direct op een aangesloten toestel
./gradlew test                 # unit tests voor streak- en datumlogica
```

Er is geen Play Store-distributie: sideload de APK. Play Protect klaagt over een
onbekende bron; dat is bij een eigen build te verwachten.

## Instellen na installatie

Alle vier de permissies zijn "special access" — er is geen runtime-dialog, je zet ze
zelf aan. Het dashboard toont een checklist met een knop naar het juiste
instellingenscherm:

1. **Toegang tot gebruiksgegevens** — anders blijft de schermtijd op nul staan.
2. **Toegankelijkheidsdienst** — anders verschijnt het tussenscherm nooit.
3. **Over andere apps tonen** — zorgt dat het tussenscherm bovenop mag komen.
4. **Batterij op "Onbeperkt"** — zonder dit pauzeert Android de bewaker na verloop
   van tijd en stopt de interventie er stilletjes mee.

## Structuur

```
com.tkriek.scrollless
├── data
│   ├── UsageStatsHelper.kt      schermtijd uit UsageStatsManager
│   ├── AppDatabase.kt           Room-database
│   ├── ScrollLessRepository.kt   events + dagcijfers + streak
│   ├── Settings.kt              wachttijd, daglimiet, rustperiode
│   ├── TrackedApp.kt            de gevolgde apps
│   ├── dao/                     Room-DAO's
│   └── entities/                AppOpenEvent, DailyStat
├── service
│   └── ScrollGuardAccessibilityService.kt
├── ui
│   ├── MainActivity.kt          dashboard
│   ├── DashboardScreen.kt       Compose-UI
│   ├── DashboardViewModel.kt
│   ├── InterventionActivity.kt  het tussenscherm
│   └── theme/
├── util
│   ├── Alternatives.kt          je vervangers-lijst
│   ├── Gamification.kt          streak en punten
│   └── DateUtils.kt
└── work
    └── UsageSyncWorker.kt       periodieke sync
```

## Je vervangers aanpassen

`util/Alternatives.kt` bevat tien suggesties. Houd ze concreet en persoonlijk —
"5 min aan je portfolio" werkt, "iets nuttigs doen" niet. Pas de lijst aan zodra je
merkt welke suggesties je in de praktijk wel en niet oppakt.

## Instellingen in de app

- **Bewaker aan/uit** — zet de interventie tijdelijk stil.
- **Wachttijd** — hoe lang "Toch doorgaan" geblokkeerd blijft (standaard 8 seconden).
- **Doorgegane opens per dag** — je eigen grens voor een goede dag (standaard 2).

Na "Toch doorgaan" zwijgt de bewaker vijf minuten, anders krijg je het scherm bij
elke schermwissel binnen de app te zien.

## Privacy

De database staat in de app-sandbox en verlaat het toestel niet. De
toegankelijkheidsdienst leest geen scherminhoud (`canRetrieveWindowContent=false`) en
krijgt alleen events van de twee gevolgde apps.
