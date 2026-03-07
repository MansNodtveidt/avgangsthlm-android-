# AvgångSthlm

> ⚠️ Denna app är INTE officiellt associerad med SL, Region Stockholm
> eller Trafiklab. Det är ett hobbyprojekt byggt med öppen data.

En Android-app som visar nästa avgångar från dina favoritstållplatser
i Stockholms kollektivtrafik – direkt på hemskärmen via en widget.

## Funktioner

- Visa nästa avgångar i realtid från valfri hållplats
- Hemskärmswidget med avgångstider och minuter kvar
- Sparade favoriter (hållplats + linje + riktning)
- Auto mode – byter favorit automatiskt baserat på tid och dag
- Fungerar med tunnelbana, buss, spårväg och pendeltåg

## Skärmdumpar

*(lägg till skärmdumpar här)*

## Projektstruktur

```
app/src/main/java/com/example/avgngsthlm/
├── data/
│   ├── local/
│   │   ├── dao/            # FavoriteDao, AutoRuleDao
│   │   ├── db/             # AppDatabase (Room)
│   │   └── entity/         # Favorite, AutoRule
│   ├── remote/
│   │   ├── model/          # DeparturesResponse, PlacesResponse
│   │   ├── ApiConstants.kt
│   │   ├── RetrofitClient.kt
│   │   └── SLApiService.kt
│   ├── repository/         # FavoriteRepository, AutoRuleRepository, DeparturesRepository
│   └── AppSettings.kt      # DataStore-inställningar
├── ui/
│   ├── navigation/         # AppNavigation (bottom nav + NavHost)
│   ├── screens/
│   │   ├── favorites/
│   │   ├── addfavorite/
│   │   ├── automode/
│   │   ├── widgetpreview/
│   │   └── help/
│   └── theme/
├── util/
│   └── AutoModeHelper.kt   # Logik för att matcha regler mot aktuell tid
├── widget/
│   ├── AvgangWidget.kt
│   ├── AvgangWidgetReceiver.kt
│   ├── RefreshWidgetCallback.kt
│   └── WidgetKeys.kt
├── worker/
│   └── WidgetUpdateWorker.kt   # WorkManager – uppdaterar widgeten var 15 min
└── MainActivity.kt
```

## Teknisk stack

| Komponent | Bibliotek |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Widget | Glance AppWidget |
| Bakgrundsarbete | WorkManager |
| Nätverksanrop | Retrofit + OkHttp |
| Lokal databas | Room |
| Inställningar | DataStore |
| Navigering | Navigation Compose |

## API

Appen använder [ResRobot v2.1](https://www.trafiklab.se/api/trafiklab-apis/resrobot-v21/)
via Trafiklab för att hämta avgångar och söka hållplatser.

| Endpoint | Beskrivning |
|---|---|
| `GET /departureBoard` | Nästa avgångar för en hållplats |
| `GET /location.name` | Autocomplete-sökning på hållplatsnamn |

## Kända begränsningar

- Minsta uppdateringsintervall för WorkManager är 15 minuter (Android-begränsning)
- Riktningsfiltret matchas med textjämförelse på klientsidan
- Samsung-enheter kan kräva manuell inaktivering av batteribegränsningar

---
---

# AvgångSthlm (English)

> ⚠️ This app is NOT officially associated with SL, Region Stockholm,
> or Trafiklab. It is a hobby project built with open data.

An Android app that shows the next departures from your favourite stops
in Stockholm public transit — directly on your home screen via a widget.

## Features

- Real-time next departures from any stop
- Home screen widget with departure times and minutes remaining
- Saved favourites (stop + line + direction)
- Auto mode — automatically switches favourite based on time and day
- Works with metro, bus, tram, and commuter rail

## Screenshots

*(add screenshots here)*

## Tech Stack

| Component | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Widget | Glance AppWidget |
| Background work | WorkManager |
| Networking | Retrofit + OkHttp |
| Local database | Room |
| Settings | DataStore |
| Navigation | Navigation Compose |

## API

The app uses [ResRobot v2.1](https://www.trafiklab.se/api/trafiklab-apis/resrobot-v21/)
via Trafiklab to fetch departures and search for stops.

| Endpoint | Description |
|---|---|
| `GET /departureBoard` | Next departures for a stop |
| `GET /location.name` | Autocomplete search for stop names |

## Requirements

- Android 8.0 (API 26) or later
- Internet connection
- Background run permission (for widget updates)

## Known Limitations

- Minimum WorkManager update interval is 15 minutes (Android constraint)
- Direction filter is matched client-side by text comparison
- Samsung devices may require manually disabling battery restrictions

## Disclaimer

Departure data is provided by [Trafiklab](https://trafiklab.se) /
[ResRobot](https://www.trafiklab.se/api/trafiklab-apis/resrobot-v21/).
This project is not affiliated with or endorsed by SL or Region Stockholm.
