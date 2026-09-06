# 🥗 Swock – KI-gestützter Ernährungstracker für Android

[![Android](https://img.shields.io/badge/Android-17%20(API%2037)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Google Gemini](https://img.shields.io/badge/AI-Gemini%20Flash%20%26%20Imagen%203-8E75B2?logo=google&logoColor=white)](https://ai.google.dev)
[![Health Connect](https://img.shields.io/badge/Sync-Android%20Health%20Connect-009688?logo=googlefit&logoColor=white)](https://developer.android.com/health-and-fitness/guides/health-connect)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Swock** ist eine moderne, datenschutzfreundliche Android-App zur intelligenten Erfassung und Auswertung deiner täglichen Ernährung. Durch die Kombination aus **Google Gemini Multimodal AI**, **Google Imagen 3**, **Android Health Connect** und **Google Drive Cloud-Backup** macht Swock das Kalorientracken so mühelos und aufschlussreich wie nie zuvor.

---

## 🌟 Highlights & Funktionen

### 📸 Multimodales Tracking: Foto oder Text
* **Foto-Erkennung in Sekunden**: Fotografiere dein Essen oder wähle ein Bild aus der Galerie. Die multimodale Gemini-KI analysiert die Mahlzeit, schätzt Portionsgrößen und berechnet Kalorien sowie Makronährstoffe (Protein, Kohlenhydrate, Fett).
* **Text-Erfassung mit KI-Generierung**: Hast du kein Foto zur Hand? Beschreibe deine Mahlzeit einfach in Textform (z. B. *"2 Scheiben Vollkornbrot mit Avocado und Spiegelei"*). Die KI schätzt die Werte und erzeugt automatisch:
  * Entweder ein ansprechendes 3D-Food-Icon über **Google Imagen 3**,
  * oder ein sofort gerendertes, thematisch abgestimmtes Vektor-/Canvas-Icon über den integrierten **`SymbolicFoodImageGenerator`**.
* **Präzise Feinabstimmung**: Vor dem Speichern können Portionsgröße, Name, Makros und Notizen in einem übersichtlichen Dialog kontrolliert und manuell angepasst werden.

### 🚦 Intelligentes Gesundheits-Ampelsystem
* Jede Mahlzeit wird ernährungsphysiologisch bewertet:
  * 🟢 **Grün (Gesund)**: Nährstoffreich, ballaststoffreich, ausgewogen.
  * 🟡 **Gelb (Moderat)**: In Maßen genießen, durchschnittliche Nährstoffdichte.
  * 🔴 **Rot (Sparsam)**: Hohe Kalorien- oder Zuckerdichte, Genussmittel.
* Zu jeder Bewertung liefert die KI eine fundierte **Begründung und praktische Ernährungstipps**.

### ⌚ Dynamisches Kalorienbudget via Health Connect
* **Automatischer Smartwatch-Sync**: Nahtlose Anbindung an Android **Health Connect** (kompatibel mit Pixel Watch, Samsung Galaxy Watch, Google Fit, Withings etc.).
* **Echtzeit-Anpassung**: Ausgelesene Aktiv-Kalorien erhöhen dein Kalorienbudget für den Tag dynamisch. Wenn du trainierst, siehst du sofort, wie viel zusätzlicher Spielraum dir zur Verfügung steht.

### 🎯 Wissenschaftliche Kalorienbedarfs-Berechnung
* **Grundumsatz (BMR)**: Exakt berechnet nach der wissenschaftlich anerkannten **Mifflin-St Jeor-Formel** basierend auf Alter, Geschlecht, Größe und Gewicht.
* **Gesamtumsatz (TDEE)**: Individuell skaliert nach PAL-Aktivitätsfaktoren (von sitzendem Bürojob bis Leistungssport).
* **Flexible Zielvorgaben**:
  * Gewicht halten (100% Gesamtumsatz)
  * Leichtes Defizit (-300 kcal) oder Standard-Fettabbau (-500 kcal)
  * Leichter Aufbau (+300 kcal) oder Muskelaufbau (+500 kcal)

### 📊 Dashboard & Tagebuch-Verlauf
* **Tages-Dashboard**: Interaktiver Donut-Kalorienring, Statusindikator (Kalorienziel erreicht / überschritten) und Makro-Fortschrittsbalken.
* **Historie**: Bequemes Blättern zwischen Tagen über Kalenderwähler oder Navigationspfeile. Mahlzeiten können jederzeit eingesehen oder gelöscht werden.

### 💾 Backup & Cloud-Synchronisation
* **Lokaler ZIP-Export & Import**: Sichere alle deine Mahlzeiten, Profil-Einstellungen und Originalfotos in einer komprimierten ZIP-Datei auf deinem Gerätespeicher.
* **Google Drive Cloud-Backup**: Sichere Speicherung im privaten AppData-Ordner deines Google Drive Kontos.
* **Automatisches Hintergrund-Backup**: Zuverlässig gesteuert durch den Android `WorkManager` (konfigurierbares Intervall, z. B. täglich, optional ausschließlich bei aktiver WLAN-Verbindung).

### 📱 Modernes Android 17 Design
* **100% Jetpack Compose & Material 3** mit harmonischem Farbschema, dynamischem Dark Mode und flüssigen Animationen.
* **Edge-to-Edge Display**: Optimiert für die neuesten Android 15/16/17 Systemleisten und Gesten.
* **Predictive Back Navigation**: Vollständige Unterstützung für vorausschauende Zurück-Gesten.
* **Haptisches Feedback & Responsive Design**: Angepasst für Hoch- und Querformat (Landscape-optimiert).

---

## 🔒 Datenschutz & Privatsphäre

* **100% Offline-First SQLite-Speicher**: Alle deine Mahlzeiten, Fotos und Profileinstellungen werden lokal auf deinem Smartphone in einer SQLite-Datenbank gespeichert.
* **Keine Tracking-Server**: Es gibt kein proprietäres Tracking-Backend. Bild- und Textanalysen werden ausschließlich direkt verschlüsselt an die offizielle Google Gemini API übertragen.
* **Isolierter Cloud-Speicher**: Das Google Drive Backup verwendet den geschützten `drive.appdata`-Bereich, auf den andere Apps keinen Zugriff haben.

---

## 🛠 Technologie-Stack

| Bereich | Technologie / Bibliothek |
| :--- | :--- |
| **Plattform** | Android (minSdk 37, targetSdk 37, compileSdk 37) |
| **Programmiersprache** | Kotlin 2.0+ (Java 17 Bytecode) |
| **UI-Framework** | Jetpack Compose mit Material 3 (`androidx.compose.material3`) |
| **Architektur** | MVVM (Model-View-ViewModel), StateFlow, Coroutines |
| **Lokale Datenbank** | SQLite (`SQLiteOpenHelper`) für volle Unabhängigkeit und Geschwindigkeit |
| **Netzwerk & HTTP** | OkHttp 4 |
| **Künstliche Intelligenz** | Google Gemini API (`gemini-3.6-flash`, Fallback auf `2.5-flash` / `1.5-flash`), Google Imagen 3 (`imagen-3.0-generate-002`) |
| **Fitness / Health** | AndroidX Health Connect Client (`androidx.health.connect:connect-client`) |
| **Cloud & Auth** | Google Play Services Auth (`play-services-auth`), Google Drive REST API v3 |
| **Hintergrundprozesse**| AndroidX WorkManager (`androidx.work:work-runtime-ktx`) |
| **Bildanzeige** | Coil Compose (`io.coil-kt:coil-compose`) & Android Native Canvas Graphics |

---

## 📁 Projektstruktur

```
swock/
├── app/
│   ├── src/main/
│   │   ├── java/de/carcophan/swock/
│   │   │   ├── MainActivity.kt               # Haupt-Activity mit Compose Navigation & Result Launchern
│   │   │   ├── data/
│   │   │   │   ├── backup/                   # BackupManager, Drive Service, Auth & WorkManager
│   │   │   │   ├── health/                   # HealthConnectManager für Kaloriensync
│   │   │   │   ├── local/                    # SQLite Helper (MealDatabaseHelper)
│   │   │   │   ├── preferences/              # UserProfileRepository (SharedPreferences)
│   │   │   │   ├── remote/                   # GeminiFoodService (Gemini Vision & Imagen 3)
│   │   │   │   └── repository/               # MealRepository mit StateFlow
│   │   │   ├── domain/
│   │   │   │   └── CalorieCalculator.kt      # Mifflin-St Jeor & TDEE Berechnungen
│   │   │   ├── model/
│   │   │   │   ├── MealEntry.kt              # Datenmodelle für Mahlzeiten & Ampel-Ratings
│   │   │   │   └── UserProfile.kt            # Benutzerprofil, Aktivitätslevel, Ziele
│   │   │   ├── ui/
│   │   │   │   ├── MainViewModel.kt          # Zentraler UI-Zustand & Business-Logik
│   │   │   │   ├── common/                   # Shared UI-Komponenten (z. B. HealthTrafficLight)
│   │   │   │   ├── confirm/                  # Bestätigungs- & Nachbearbeitungs-Dialog
│   │   │   │   ├── dashboard/                # Tagesübersicht, Kalorienring & Text-Erfassungs-Dialog
│   │   │   │   ├── history/                  # Verlaufsansicht aller Tage
│   │   │   │   ├── settings/                 # Profil-, API-Key-, Health-Connect- & Backup-Settings
│   │   │   │   └── theme/                    # Material 3 Farbpaletten & Typografie
│   │   │   └── util/
│   │   │       └── SymbolicFoodImageGenerator.kt # Lokaler Canvas-Renderer für Speisen-Icons
│   │   ├── res/                              # Layout-Ressourcen, App-Icons, XML-Konfigurationen
│   │   └── AndroidManifest.xml               # Permissions (Kamera, Health Connect, Internet)
│   └── build.gradle.kts                      # Modul-Build-Konfiguration & Dependencies
├── build.gradle.kts                          # Projekt-Build-Konfiguration
└── gradle.properties                         # JVM- & Gradle-Build-Eigenschaften
```

---

## 🚀 Einrichtung & Installation

### Voraussetzungen
* **Android Studio**: Android Studio Ladybug (2024.2.1+) oder neuer.
* **JDK**: Version 17 oder höher.
* **Android SDK**: Android 16/17 (API 37 SDK Platform & Build-Tools).
* **Testgerät**: Android-Gerät oder Emulator mit Android 15 / 16 / 17 (API 35–37).

### 1. Repository klonen
```bash
git clone https://github.com/Carcophan/swock.git
cd swock
```

### 2. Projekt öffnen & bauen
Öffne das Projekt in Android Studio. Gradle synchronisiert automatisch die Abhängigkeiten.

Über die Kommandozeile bauen:
```bash
./gradlew assembleDebug
```

Auf einem angeschlossenen Android-Gerät installieren:
```bash
./gradlew installDebug
```

---

## 🔑 Konfiguration & API-Schlüssel

### 1. Google Gemini API Key
Für die KI-gestützte Foto- und Textanalyse benötigst du einen kostenlosen Gemini API-Schlüssel:
1. Rufe das [Google AI Studio](https://aistudio.google.com/) auf und erstelle einen API-Key.
2. Öffne die **Swock** App und navigiere zum Tab **Profil (Einstellungen)**.
3. Füge deinen API-Schlüssel im Feld **Google Gemini API Key** ein und tippe auf **API-Key testen**.
4. Wähle das gewünschte Modell (Standard: `gemini-3.6-flash`).

> [!TIP]
> Die App verfügt über automatische Fallbacks: Sollte ein Modell temporär nicht verfügbar sein, schaltet Swock nahtlos auf alternative Gemini-Flash-Versionen um.

### 2. Health Connect (Smartwatch-Kalorien)
1. Aktiviere in den App-Einstellungen den Schalter **Health Connect synchronisieren**.
2. Gewähre der App die Berechtigungen zum Lesen der **Gesamtkalorien** und **Aktivkalorien**.
3. Deine verbrannten Kalorien werden ab sofort automatisch in die dynamische Tagesziel-Berechnung einbezogen.

### 3. Google Drive Cloud-Backup (Optional)
1. Erstelle in der [Google Cloud Console](https://console.cloud.google.com/) eine **OAuth 2.0 Client-ID** für Android:
   - **Paketname**: `de.carcophan.swock`
   - **SHA-1 Fingerabdruck**: Trage den SHA-1 deines Debug- bzw. Release-Keystores ein.
2. In der App unter **Profil > Google Drive Backup** mit deinem Google-Konto anmelden.
3. Du kannst nun jederzeit manuelle Cloud-Backups erstellen, wiederherstellen oder automatische Backups aktivieren.

---

## 📄 Lizenz

Dieses Projekt ist unter der [MIT-Lizenz](LICENSE) lizenziert.
