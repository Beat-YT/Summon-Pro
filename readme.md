# 🚗 Summon Pro — Extend Tesla Smart Summon Range

**Summon Pro** is an Android app that lifts the 85-meter geofence limit imposed by Tesla’s Smart Summon feature.  
It works by spoofing your phone’s GPS location to always appear near your Tesla, enabling full parking lot navigation — even from across town.

> ⚠️ This app does **not** drive your car. It simply removes the leash.  
> Tesla's own Autopilot/Smart Summon stack still controls all movement, braking, and obstacle detection.

---

## 🔧 How it works

Tesla Smart Summon requires your phone to be within ~85 m of the car.  
Summon Pro connects to your vehicle using the [Tesla Fleet API](https://developer.tesla.com/docs/fleet-api) and:

1. Streams live GPS location of the vehicle.
2. Calculates a spoofed phone location within ~80 m of the car (like dangling a digital “carrot on a stick”).
3. Updates your phone’s mock GPS location accordingly.
4. Tesla app thinks you’re nearby → Smart Summon stays active.

This trick allows remote Smart Summon from **virtually anywhere** — as long as the car has signal.

---

## 🧪 Status

Summon Pro is in active development and works surprisingly well already.  
I have successfully used it in large parking lots, from buildings, traveling well over 400m (~1300 ft)

---

## 📦 Download

You can download the latest APK from the official site:

👉 **[⬇️ Download APK](https://summon-pro.cc/download)**

---

## 📱 Requirements

- Android 8.0+
- Developer options enabled
- “Mock location app” set to **Summon Pro**
- Tesla account with Fleet API access
- Tesla vehicle with Smart Summon capability (FSD or EAP)

---

## 🛠 Features
- 🚗 **Custom route path planning** — drag & drop waypoints for smarter summon paths (experimental)
- 🔁 Real-time vehicle tracking via Tesla API
- 🧭 Dynamic fake GPS location near the car
- 📶 Works over Wi-Fi or cellular network
- 🚫 No root required
- 🌙 Material You theme (light/dark)
- 🔐 OAuth login flow with token storage

---

## ⚙️ Setup

1. Enable Developer Mode on your Android phone  
   *(Settings → About Phone → Tap Build Number 7x)*  
2. Go to **Developer options** → Select mock location app → choose **Summon Pro**
3. Log in with your Tesla account (OAuth — no password stored)
4. Select your vehicle and start the service
5. Open the Tesla app → Use Smart Summon as usual

---

## ❓FAQ

### Does this app drive the car?
**No.** Only Tesla’s Autopilot stack handles vehicle movement. Summon Pro only tricks the app into thinking you're nearby.

### Will this work on iPhone?
Unfortunately, no. iOS does not allow mock location apps. I'm an iPhone user too 😢

### Is this safe?
Summon Pro only manipulates GPS. Use common sense — **you’re still responsible** for the vehicle’s behavior.

### Will this work in China?
Tesla's Chinese fleet is on a different API domain. Support is planned, but you’ll need a separate developer account on [developer.tesla.cn](https://developer.tesla.cn).

---

## 🧑‍💻 For developers

This project is written in **Kotlin** and uses:

- Jetpack Navigation
- Kotlin Coroutines
- WebSockets
- Tesla OAuth 2.0 flow
- ForegroundService for GPS mocking
- Google Maps Mobile SDK

Mock location is updated using Android's `LocationManager.setTestProviderLocation`.

---

## 🧾 License

This project is licensed under the **GNU General Public License v3.0**.  
See the [LICENSE](./LICENSE) file for full terms.

---

## ❤️ Support

If you found this useful or saved you a walk, feel free to star the repo, report issues, or donate:  
[☕ Buy me a coffee](https://ko-fi.com/justjdupuis)

---

> Made for curious Tesla owners who want to push boundaries, not break rules.  
> Summon smarter. Summon farther.  
> **Summon Pro.**

