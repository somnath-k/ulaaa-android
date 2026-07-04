# Ulaaa — Explore Without Limits

A collaborative travel discovery and planning app for Android. Discover destinations,
plan trips with your Squad, organize itineraries, and save your bucket list.

## Tech Stack
- **UI:** Kotlin · Jetpack Compose · Material 3 · Navigation Compose
- **Architecture:** MVVM · Feature-based packages · StateFlow · Coroutines
- **DI:** Hilt
- **Backend:** Firebase Authentication · Cloud Firestore
- **Networking:** Retrofit · kotlinx.serialization · OkHttp
- **Local:** Room (planned)
- **Maps & Places:** Ola Maps (MapLibre vector tiles) · Geoapify · Open-Meteo
- **AI:** Gemini (planned recommendation engine)

## Modules / Packages
```
com.dotkios.ulaaa
├── data
│   ├── model         # Trip, Landmark, UserProfile, GeoPoint, …
│   ├── remote        # Retrofit APIs + DTOs (Geoapify)
│   └── repository    # Auth, User, Places, Location repositories
├── di                # Hilt modules (Firebase, Network, Repository)
└── ui
    ├── auth          # Login / Signup
    ├── splash        # Auth-gated entry
    ├── home          # Discovery feed
    ├── map           # Ola Maps + nearby landmarks
    ├── bucketlist    # Saved destinations (planned)
    ├── profile       # Account
    ├── components    # Reusable Compose components
    ├── navigation    # Root graph + bottom-nav shell
    └── theme         # Color / Typography / Shape
```

## Build Progress
- [x] Sprint 1 — Project setup, design system, bottom navigation, Home UI
- [x] Sprint 2 — Firebase Authentication + Firestore user profiles
- [x] Sprint 3 — Ola Maps, current location, nearby landmarks (Geoapify)
- [x] Sprint 4 — Trip management, Bucket List, Room
- [x] Sprint 5 — Gemini recommendations, unit tests

## Setup
This project needs local secrets that are **not** committed:

1. Add your Firebase `google-services.json` to `app/`.
2. Add API keys to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_key
   OLA_MAPS_API_KEY=your_key
   GEOAPIFY_API_KEY=your_key
   ```
3. In the Firebase console enable **Email/Password** auth and create a **Firestore** database.
4. Run: `./gradlew :app:assembleDebug`

## License
Personal portfolio project.
