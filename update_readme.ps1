$content = Get-Content -Path README.md -Raw

$badges = "
[![Latest Release](https://img.shields.io/github/v/release/d0x-dev/airbeats?style=for-the-badge&logo=github&color=0D1117&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/releases)
[![License](https://img.shields.io/github/license/d0x-dev/airbeats?style=for-the-badge&logo=gnu&color=2B3137&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/blob/main/LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%25206.0+-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white&labelColor=161B22)](https://www.android.com)
[![GitHub Stars](https://img.shields.io/github/stars/d0x-dev/airbeats?style=for-the-badge&logo=github&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/d0x-dev/airbeats?style=for-the-badge&logo=github&labelColor=161B22)](https://github.com/d0x-dev/AirBeats/network)
[![Crowdin](https://img.shields.io/badge/??_Translations-Crowdin-2E3340?style=for-the-badge&logo=crowdin&logoColor=white)](https://crowdin.com/project/airbeats)
<br><br>
<a href="https://snapcraft.io/airbeats"><img alt="Get it from the Snap Store" src="https://snapcraft.io/static/images/badges/en/snap-store-black.svg" height="45"/></a>
<a href="https://airbeats.en.uptodown.com/android"><img alt="Download on Uptodown" src="https://stc.uptodown.com/img/badges/uptodown-badge-en.png" height="45"/></a>
"

$content = $content -replace '\[\!\[Latest Release\][\s\S]*?crowdin\.com/project/airbeats\)', $badges

$desktopSection = "
---

## ?? AirBeats Desktop

AirBeats isn't just for Android! You can enjoy the exact same seamless YouTube Music integration on **Mac**, **Linux**, and **Windows**. 

Head over to our official desktop repository to get the desktop client:
?? **[d0x-dev/airbeats-desktop](https://github.com/d0x-dev/airbeats-desktop)**

### ?? Linux Users
AirBeats Desktop is officially available on the Snap Store!
\\\ash
sudo snap install airbeats
\\\

---
"

$content = $content -replace '---[\r\n\s]*## ?? Features', "$desktopSection
## ?? Features"

Set-Content -Path README.md -Value $content
