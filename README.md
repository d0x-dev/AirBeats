<p align="center">
  <img src="https://raw.githubusercontent.com/drkvenom786/Airbeats/refs/heads/main/icon2.png" width="250" alt="AirBeats Logo" />
</p>

<h1 align="center">AirBeats</h1>

<div align="center">

<img src="https://raw.githubusercontent.com/drkvenom786/Airbeats/refs/heads/main/sc.png" alt="AirBeats Preview" width="100%"/>

### 🚀 Advanced YouTube Music Client with Material Design 3

**Experience Music Like Never Before on Android**

[![Latest Release](https://img.shields.io/github/v/release/d0x-dev/airbeats?style=for-the-badge&logo=github&color=0D1117&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/releases)
[![License](https://img.shields.io/github/license/d0x-dev/airbeats?style=for-the-badge&logo=gnu&color=2B3137&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/blob/main/LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%206.0+-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white&labelColor=161B22)](https://www.android.com)
[![GitHub Stars](https://img.shields.io/github/stars/d0x-dev/airbeats?style=for-the-badge&logo=github&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/d0x-dev/airbeats?style=for-the-badge&logo=github&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/network)
[![Crowdin](https://img.shields.io/badge/𝐓𝐑𝐀𝐍𝐒𝐋𝐀𝐓𝐄-2E3340?style=for-the-badge&logo=crowdin&logoColor=white)](https://crowdin.com/project/airbeats)

</div>

---

## ✨ Features

- 🎵 **Seamless YouTube Music Integration** - Full access to your favorite tracks
- 🎨 **Material Design 3** - Modern, elegant, and intuitive UI/UX
- 🎯 **Advanced Playback Controls** - Fine-tune your music experience
- 🔐 **Secure & Privacy-Focused** - Your data stays yours
- ⚡ **Lightning Fast** - Optimized performance and smooth animations
- 🌙 **Dark Mode Support** - Easy on the eyes, anytime
- 📱 **Android 6.0+** - Works on a wide range of devices

---

## 🌐 Get Started

| Link | Purpose |
|------|---------|
| 🌍 [Official Website](http://dxv.ispro.in) | Learn more about AirBeats |
| 📥 [Download APK](http://airbeats.stormx.pw) | Get the latest version |
| 🏪 [Official Store](http://store.stormx.pw) | Alternative download source |

---

## 👥 Meet the Talented Team

### 💻 Lead Developer & Founder

**Darkboy** - Full-Stack Android Developer
- 🔗 [GitHub](https://github.com/d0x-dev)
- 🌐 [Website](https://darkboy.pro)
- 📱 [Telegram](https://t.me/songpy)
- 📸 [Instagram](https://instagram.com/dark__336)
- <img src="https://avatars.githubusercontent.com/u/218248866?s=400&u=7d12b7d4c3f4cbb804fd5080d26623e7c94f6821&v=4" width="60" style="border-radius: 50%; margin-top: 10px;"/>

### 🎨 UI/UX Specialist & Designer

**Venom** - Creative Design Visionary
- 🔗 [GitHub](https://github.com/drkvenom786)
- 🌐 [Website](http://venomx.pro)
- 🎨 [Portfolio](https://drkvenom786.github.io/webpage/)
- <img src="https://avatars.githubusercontent.com/u/241423835" width="60" style="border-radius: 50%; margin-top: 10px;"/>

---

## 🛠️ Build from Source

Ready to build AirBeats yourself? Follow our comprehensive guide below.

### Prerequisites

| Requirement | Version |
|------------|---------|
| **Java Development Kit (JDK)** | 21 |
| **Android Studio** | Ladybug or newer |
| **Git** | Latest |

### 🔑 Essential Configuration (Firebase & Google API)

> [!IMPORTANT]
> ⚠️ The Firebase configuration file (`google-services.json`) and Google API Key are **not** included in the repository for **security reasons**. You **must** configure these manually to build the project successfully.

#### Step 1️⃣: Firebase Configuration (google-services.json)

1. Visit [Firebase Console](https://console.firebase.google.com/)
2. Click **Create a new project** (or use existing)
3. Once created, click the **Android** icon
4. Register your app with these details:
   - **Package name**: `com.darkxvenom.airbeats`
   - **App nickname**: `AirBeats`
5. Click **Register app**
6. **Download** the `google-services.json` file
7. **Move** it to `AirBeats/app/google-services.json`

#### Step 2️⃣: Google API Key (local.properties)

1. Open `local.properties` in the project root
2. Add your Google API Key:
   ```properties
   google.api.key=YOUR_GOOGLE_API_KEY_HERE
   ```

### 🚀 Build Steps

```bash
# 1. Clone the repository
git clone https://github.com/d0x-dev/AirBeats.git

# 2. Navigate into the project
cd AirBeats

# 3. Clean build files
./gradlew clean

# 4. Build the Debug APK
./gradlew assembleDebug

# 5. Find your APK at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Build Variants

```bash
# Build Release APK (requires signing key)
./gradlew assembleRelease

# Build and run on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

---

## 📜 License

**Copyright © 2025 Darkboy & Venom**

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License.
```

---

## 🤝 Contributing

We'd love your contributions! Here's how:

### 🌍 Translate AirBeats

Help us translate AirBeats into your language! You don't need any coding experience to help. We use **Crowdin** to manage our translations.

1. Go to our [Crowdin Project Page](https://crowdin.com/project/airbeats).
2. Create a free account.
3. Select your native language and start translating! All translations will be automatically synced to our GitHub repository.

### 💻 Code Contributions

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

---

## 💬 Support & Community

- 📧 Have questions? Open an [Issue](https://github.com/d0x-dev/AirBeats/issues)
- 🐛 Found a bug? Report it [here](https://github.com/d0x-dev/AirBeats/issues/new)
- 💡 Have a suggestion? We'd love to hear it!

---

<div align="center">

### Made with ❤️ by the AirBeats Team

⭐ If you love AirBeats, please consider giving us a star on GitHub! ⭐

[⬆ Back to Top](#-airbeats-)

</div>
