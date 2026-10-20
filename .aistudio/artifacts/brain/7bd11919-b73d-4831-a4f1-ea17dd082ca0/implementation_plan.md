# Rebrand App Identity from WordBooks to Qaf (قاف) & Custom Adaptive Icon

Update the application's identity across the Android launcher, AI Studio project metadata, and in-app navigation headers to **Qaf**, paired with a custom vector adaptive launcher icon styled in the app's signature dark obsidian navy (`#0B0F17`) and warm amber gold (`#F59E0B`) color palette.

---

## 1. Three Custom Icon Concepts for Qaf (Dark Navy `#0B0F17` & Warm Gold `#F59E0B`)

*(Note: Because the daily free-tier quota for the AI image generator was reached, the icon will be crafted as an ultra-crisp, resolution-independent vector adaptive icon and rendered to high-DPI launcher PNGs across all device densities).*

### Concept 1 (Recommended Default): Golden Arabic Qaf + Open Book Emblem
- **Visual Design**: A bold, sculpted Arabic letter **ق (Qaf)** in luminous warm gold (`#F59E0B`) rising seamlessly from the center of a stylized geometric open book base, accented by two floating diamond-shaped golden nuqat (dots) above the loop and a subtle emerald-gold inner glow on a deep obsidian navy (`#0B0F17`) background.
- **Vibe**: Scholarly, modern, and unmistakably tied to bilingual reading and vocabulary mastery.

### Concept 2: Royal Geometric Qaf Calligraphy Crest
- **Visual Design**: A modern Kufic/geometric Arabic **ق (Qaf)** emblem enclosed within a sleek octagonal golden seal frame with hairline amber borders (`#F59E0B` / `#FBBF24`) and warm radial depth on the `#0B0F17` canvas.
- **Vibe**: Prestigious, minimal, and distraction-free on the phone home screen.

### Concept 3: Dual Arabic-Latin "ق × Q" Monogram
- **Visual Design**: An intertwined bilingual monogram where the sweeping tail of the Arabic **ق** forms the circular ring and tail of the Latin letter **Q**, symbolizing the bridge between Arabic and English fluency.
- **Vibe**: Contemporary fintech-grade minimalism with a strong bilingual identity.

*(Proceeding with this plan will implement **Concept 1** by default, or you can mention Concept 2 or Concept 3 in feedback if you prefer either alternative).*

---

## 2. Unified Brand Identity Updates ("Qaf")

### Launcher & Platform Identity
- Update the Android home screen launcher label to **Qaf**.
- Synchronize the AI Studio project name and root project identifier to **Qaf** so the sidebar, APK package metadata, and home screen icon label match everywhere.

### In-App Top Bar & Shared Labels
- Update the main dashboard header title from **WordBooks** to **Qaf** (with the subtitle **قاف • Learning Dashboard**).
- Update clipboard and share labels across sentence and dictionary tools to reference **Qaf**.

### Adaptive & Multi-Density Launcher Icons
- Replace both the adaptive foreground/background layers and all legacy density launcher icons (`mdpi` through `xxxhdpi`, square and round masked) with the new **Qaf** dark-and-gold icon so physical phones and emulators display the new branding immediately upon install.
