# AirBeats 亗

<div align="center">
  <img src="https://raw.githubusercontent.com/drkvenom786/Airbeats/refs/heads/main/sc.png" alt="AirBeats Preview" width="100%"/>
  
  ### Advanced YouTube Music Client with Material Design 3 for Android
  
  [![Latest Release](https://img.shields.io/github/v/release/d0x-dev/airbeats?style=flat-square&logo=github&color=0D1117&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/releases)
  [![License](https://img.shields.io/github/license/d0x-dev/airbeats?style=flat-square&logo=gnu&color=2B3137&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/blob/main/LICENSE)
  [![Android](https://img.shields.io/badge/Platform-Android%206.0+-3DDC84.svg?style=flat-square&logo=android&logoColor=white&labelColor=161B22)](https://www.android.com)
</div>

---

## 🌐 Official Links

- **Official Website**: [dxv.ispro.in](http://dxv.ispro.in)
- **APK Download**: [airbeats.stormx.pw](http://airbeats.stormx.pw)
- **Official Store**: [store.stormx.pw](http://store.stormx.pw)

---

## 👥 Meet the Team

We are a group of developers dedicated to bringing you the best music experience on Android:

### 💻 Lead Developer
* **Darkboy**
  * **GitHub Profile**: [@d0x-dev](https://github.com/d0x-dev)
  * **Website**: [darkboy.pro](https://darkboy.pro)
  * **Telegram**: [t.me/songpy](https://t.me/songpy)
  * **Instagram**: [@dark__336](https://instagram.com/dark__336)
  * **Avatar**: 
    <img src="https://avatars.githubusercontent.com/u/218248866?s=400&u=7d12b7d4c3f4cbb804fd5080d26623e7c94f6821&v=4" width="80" style="border-radius: 50%"/>

### 🎨 UI/UX Specialist
* **Venom**
  * **GitHub Profile**: [@drkvenom786](https://github.com/drkvenom786)
  * **Website**: [venomx.pro](http://venomx.pro)
  * **Web Portfolio**: [venomx portfolio](https://drkvenom786.github.io/webpage/)
  * **Avatar**: 
    <img src="https://avatars.githubusercontent.com/u/241423835" width="80" style="border-radius: 50%"/>

---

## 🛠️ Building from Source

To compile and build AirBeats on your own, please follow this step-by-step build guide.

### 1. Prerequisites

- **Java Development Kit (JDK)**: JDK 21 is required.
- **Android Studio**: Android Studio (Ladybug or newer recommended).
- **Git**: Installed on your development environment.

### 2. Firebase & Google API Key Configuration (Crucial Step)

> [!IMPORTANT]
> The Firebase configuration file (`google-services.json`) and the Google API Key are **not** included in this repository for security reasons. You must add them manually to build the project.

#### A. Firebase Configuration (google-services.json)
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Create a project** (or select an existing project).
3. Once the project is created, click the **Android** icon on the dashboard to register an app.
4. Input the following details:
   - **Android package name**: `com.darkxvenom.airbeats`
   - **App nickname**: `AirBeats`
5. Click **Register app**.
6. Download the generated **`google-services.json`** file.
7. Move the `google-services.json` file directly into the **`app/`** directory of the project (i.e. `AirBeats/app/google-services.json`).

#### B. Google API Key Configuration (local.properties)
To enable YouTube PoToken generation (WebView-based BotGuard client):
1. Open the **`local.properties`** file in the root directory of this project.
2. Add your Google API Key under the property name `google.api.key`:
   ```properties
   google.api.key=YOUR_GOOGLE_API_KEY_HERE
   ```

### 3. Cloning & Building

```bash
# Clone the repository
git clone https://github.com/d0x-dev/AirBeats.git

# Navigate to the directory
cd AirBeats

# Clean compile files
./gradlew clean

# Build the Debug APK
./gradlew assembleDebug
```

After compilation, the built debug APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📜 License

**Copyright © 2025 Darkboy & Venom**

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License. See the [GNU General Public License](https://github.com/d0x-dev/AirBeats/blob/main/LICENSE) for more details.
