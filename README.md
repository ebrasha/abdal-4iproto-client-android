# 🛡️ Abdal 4iProto Android

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.20635704.svg)](https://doi.org/10.5281/zenodo.20635704)

📖 [فارسی (Persian)](README.fa.md) | **English**


<p align="right">
  <img src="shot.jpg" alt="Abdal 4iProto Android" align="right" />
</p> 

**Secure, Fast, and Smart device‑wide SSH Tunneling for Android — your personal gateway to encrypted, private internet access.**

Abdal 4iProto Android is the official Android client of the **Abdal 4iProto** ecosystem. It builds a device‑wide VPN on top of the SSH‑based **4iProto** protocol and routes *all* of your phone's traffic through an encrypted tunnel to your own **Abdal 4iProto Server** — no browser extension, no per‑app proxy juggling, just one tap.

---

## 🧬 Part of the Abdal 4iProto Ecosystem

Abdal 4iProto is a complete, cross‑platform ecosystem built on top of the SSH protocol — engineered for secure tunneling, advanced management, and real‑time traffic monitoring. The ecosystem integrates multiple interconnected components, each designed for performance, scalability, and security.

> 🧬 **The Abdal 4iProto Ecosystem** — A Secure, Fast, and Modular SSH‑based Tunneling Ecosystem — Built by Abdal, Led by **Ebrahim Shafiei (EbraSha)**.

**This repository is the Android component of that ecosystem.** The other parts are:

- 🖥️ **Server (Linux/Windows):** [github.com/ebrasha/abdal-4iproto-server](https://github.com/ebrasha/abdal-4iproto-server)
- 🪟 **Windows Client:** [github.com/ebrasha/abdal-4iproto-client](https://github.com/ebrasha/abdal-4iproto-client)
- 🤖 **Android Client:** *this project*

---

## 💡 Why This App Was Created

In many networks, your ISP can see which websites and services you visit (largely through DNS and SNI), throttle or block content, and inspect traffic via Deep Packet Inspection (DPI). Desktop SSH tunneling usually requires running a local SOCKS5 proxy and manually pointing each tool (browser, etc.) at it — which is inconvenient on mobile and leaves many apps untunneled.

Abdal 4iProto Android solves this by turning your phone into a fully tunneled device:

- 🔒 It captures the traffic of **every app** at the OS level and sends it through an encrypted SSH tunnel.
- 🌐 It performs **remote DNS** on the server, so your ISP can't learn the domains you browse.
- 🧠 It keeps the experience **simple** — connect once and the whole device is protected.

The goal is private, censorship‑resistant, and secure internet access using **your own server**, with full control over your data.

---

## 🚀 Features

- 📡 **Full UDP support** — the UDP transport layer is **fully operational and production-ready**; UDP traffic is handled end-to-end through the tunnel with complete stability, enabling latency-sensitive workloads such as **VoIP**, **online gaming**, **DNS queries**, and **real-time streaming**.
- 🔐 **SSH‑based 4iProto protocol** — fully encrypted, authenticated tunnel.
- 📱 **Device‑wide VPN** — routes traffic of all apps using Android's `VpnService` (no root required).
- 🧩 **Local SOCKS5 bridge over SSH** — a built‑in SOCKS5 proxy forwards connections through SSH `direct-tcpip` channels.
- ⚡ **Native tun2socks engine** — high‑performance TUN→SOCKS bridging via `hev-socks5-tunnel`.
- 🧬 **Fake‑IP / FakeDNS (toggle)** — resolves domains **on the server** (remote DNS) so no real DNS query ever leaves the device. Can be turned on/off from the menu; the classic DNS‑over‑TCP behavior is preserved when off.
- 🛰️ **DNS through the tunnel** — DNS is tunneled (DNS‑over‑TCP or fake‑IP), never sent in the clear to your ISP.
- 🧱 **Kill Switch** — if the tunnel drops unexpectedly (e.g. the server restarts) and you haven't pressed Disconnect, internet traffic is blocked until the tunnel is restored or you disconnect — preventing leaks.
- 🪢 **Split tunneling** — private/LAN ranges (e.g. `192.168.x.x`, `10.x.x.x`) automatically bypass the tunnel, so local devices (FTP server on the phone, router, printers, casting) keep working while connected.
- ✅ **Whitelist IPs / CIDR** — add single IPs or CIDR blocks in *Advanced Settings* to bypass the tunnel on demand.
- ⏱️ **Configurable ping interval** — in *Advanced Settings*, set how often the home screen measures server latency (default 10 s; lower values update faster but use more network/battery).
- 📲 **Per-App Split Tunneling** — a dedicated *Per-App Split Tun* screen lets you decide which apps use the tunnel. **Route via Tunnel** mode tunnels only the apps you pick, while **Bypass Tunnel** mode tunnels every app except the ones you pick. A searchable list shows your installed apps, each with its own toggle. Apps kept off the tunnel are also exempt from the Kill Switch, so they keep normal connectivity at all times.
- 🔁 **Auto‑Reconnect** — automatically reconnects with smart exponential backoff if the connection drops.
- 📜 **Real‑time in‑app logs** — watch exactly what happens behind the scenes, with copy and clear actions.
- 🗂️ **Multiple named servers** — manage several servers and select the active one with a friendly name.
- 🆔 **Custom client identity** — presents `SSH-2.0-Abdal-4iProto-Android` to the server.
- 🔑 **Broad algorithm support** — wide key‑exchange/host‑key/cipher negotiation, including `ssh-ed25519` (via Bouncy Castle) for maximum server compatibility.
- 🎨 **Modern UI** — clean Material 3 (Jetpack Compose) interface with a hamburger menu for quick access to all features.
- 📊 **Redesigned home dashboard** — connection timer (always visible), large connect button with status, active **server card** (flag, name, endpoint, live ping, 4iProto badge, port-mode toggle), **traffic chart** (download/upload), and **session stats** (latency, peak speed, data used today).
- 🏳️ **Country flags & multi-port servers** — each server stores a two-letter ISO country code with flag icons; multiple SSH ports (comma-separated) are supported with **fixed** or **random port on connect** (the port is chosen only when you tap Connect, not while idle).
- 📶 **Live latency (ping)** — periodic TCP probes to the selected server with color-coded quality on the server card and stats panel; probe interval is configurable in **Advanced Settings** (default **10 seconds**, range 5–120 s).
- 📈 **Real-time traffic chart** — smooth dual-series area chart (download green / upload blue) driven by native `hev-socks5-tunnel` statistics.
- 🌐 **UDP Tunnel (enabled by default)** — transparent UDP routing and fixed UDP forwarder over the proprietary `direct-udpip` SSH channel; toggle and fine-tune from the menu (**UDP Tunnel** screen). 
- ↩️ **Back closes the drawer** — when the hamburger menu is open on the home screen, the system Back button closes the menu instead of exiting the app.
- 🙈 **No telemetry** — the app does not collect or send your data anywhere; you connect only to your own server.

---

## 🔐 Why This App Is Secure

- 🔒 **End‑to‑end SSH encryption:** all tunneled traffic is encrypted between your device and your server, so the ISP only sees an SSH connection — not your destinations or content.
- 🧬 **No DNS leak (with Fake‑IP):** domains are resolved on the server, so your ISP cannot profile the sites you visit through DNS.
- 🧱 **Leak‑proof Kill Switch:** on an unexpected tunnel drop, traffic is blocked (atomically, with no leak gap) until reconnect or manual disconnect.
- 🛡️ **Protected control connection:** the SSH control socket is protected from the VPN (`VpnService.protect`) to avoid routing loops and ensure reliable reconnects.
- 🏠 **LAN stays local:** split tunneling keeps private ranges off the tunnel, so local services are never exposed to the remote server.
- 🧾 **Transparent & auditable:** real‑time logs let you verify exactly what the tunnel is doing, and the project is open source.
- 🪪 **Your own server:** you control both ends of the tunnel — there is no third‑party VPN provider in the middle.

---

## 📲 How To Use

1. 📥 **Install** the app and open it.
2. ➕ **Add a server:** open the hamburger menu → **Server Management** → **Add Server**, then enter a friendly name, your Abdal 4iProto Server IP/hostname, port, username, and password.
3. ✅ **Select** the server on the main screen (it is shown by its name).
4. ⚙️ **(Optional) Configure** from the hamburger menu:
   - **Fake‑IP/FakeDNS** — enable for server‑side DNS resolution.
   - **Kill Switch** — enable to block traffic if the tunnel drops.
   - **Advanced Settings → Whitelist IPs / CIDR** — add comma‑separated IPs or CIDR blocks that should bypass the tunnel.
5. 🔘 **Tap Connect.** Grant the VPN permission when Android asks. Once connected, all traffic is routed securely through your SSH tunnel.
6. 📜 **View logs** anytime from the menu to monitor the connection.
7. ⏹️ **Tap Disconnect** to stop the tunnel.

> ℹ️ Connection options (Fake‑IP, Kill Switch, Whitelist) are applied when you connect, so toggle them before tapping Connect.

---

## 🧰 Technical Overview (For Developers)

**Build toolchain**

- 🐘 **Gradle (wrapper):** `9.3.1`
- 🤖 **Android Gradle Plugin (AGP):** `9.1.1`
- 🟣 **Kotlin:** `2.2.10`
- ⚙️ **KSP:** `2.3.5`
- ☕ **Java compatibility (source/target):** `11`

**Android SDK / API levels**

- 🛠️ **compileSdk:** `36` (Android 16)
- 🎯 **targetSdk:** `36`
- 📉 **minSdk:** `26` (Android 8.0)
- 📦 **applicationId:** `net.abdal.abdal4iproto.client` · **versionName:** `5.2` (versionCode `52`)

**Platform APIs used**

- 🌐 **`android.net.VpnService`** — device‑wide TUN interface, routing, `protect()`, and (API 33+) `excludeRoute()` for split tunneling.
- 🧵 **JNI / native libraries** — `hev-socks5-tunnel` (`.so`) tun2socks engine bridged via JNI (`TProxyService`).
- 🔔 **Foreground Service** — `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` + `POST_NOTIFICATIONS`.

**Core libraries**

- 🎨 **Jetpack Compose** — BOM `2024.09.00`, Material 3, Navigation Compose `2.8.9`, Activity Compose `1.10.1`, Lifecycle `2.8.7`.
- 🔐 **SSH:** JSch (mwiede fork) `0.2.21`.
- 🧮 **Crypto:** Bouncy Castle `bcprov-jdk18on:1.84` (for `ssh-ed25519` host keys).
- 🔄 **Coroutines:** kotlinx‑coroutines `1.10.2`.
- 🗄️ **Persistence:** Room `2.7.0` (KSP).
- 🌍 **Networking/serialization:** Retrofit `2.12.0`, Moshi `1.15.2`, OkHttp `4.10.0`, kotlinx‑serialization‑json `1.6.3`.
- 🧪 **Testing:** JUnit `4.13.2`, Robolectric `4.16.1`, Roborazzi `1.59.0`, Espresso `3.7.0`.

---

## 🐛 Reporting Issues

If you encounter any issues or have configuration problems, please reach out via email at Prof.Shafiei@Gmail.com. You can also report issues on GitLab or GitHub.

## ❤️ Donation

If you find this project helpful and would like to support further development, please consider making a donation:
- [Donate Here](https://t.me/AbdalDonationBot)

## 🤵 Programmer

Handcrafted with Passion by **Ebrahim Shafiei (EbraSha)**
- **E-Mail**: Prof.Shafiei@Gmail.com
- **Telegram**: [@ProfShafiei](https://t.me/ProfShafiei)

## 📜 License


This project is licensed under the **GNU AGPLv3** with additional terms
as permitted by Section 7 of the AGPLv3. See [LICENSE](./LICENSE) for the
complete text including additional terms.

### Summary of Your Rights and Obligations

- ✅ You **may** use, study, modify, and redistribute this software under
  the terms of AGPLv3.
- ✅ You **may** create derivative works, provided you comply with the
  attribution and renaming requirements below.
- ⚠️ **Network use** triggers source disclosure obligations (AGPLv3 §13).
  If you run a modified version of this software as a network service,
  you must offer the modified source code to its users.
- ⚠️ The names **"Abdal 4iProto Android"**, **"Abdal"**, **"EbraSha"**, **"Abdal Security Group"**, **"Nahaanbin CyberSecurity Company"** and associated logos are **trademarks** of
  Ebrahim Shafiei (EbraSha) and are **NOT** covered by the AGPLv3 license.
- ⚠️ Forks and modified versions **MUST be renamed** to a name that is
  not confusingly similar to the Project Brand, and may NOT reuse the
  original branding, logos, or visual identity.
- ⚠️ **All author attributions, copyright notices, "About" screens,
  credit lines, and identifying information MUST be preserved** in any
  modified version. Removal or obfuscation is a material violation of
  the License.
- ⚠️ Modified versions must clearly indicate they are modified and must
  not be represented as the official version.

For details, see the **Additional Terms** section in the [LICENSE](./LICENSE) file.

### Commercial / Trademark Licensing

For commercial licensing, trademark licensing, or permissions beyond
the scope of AGPLv3, please contact:

- **Author:** Ebrahim Shafiei (EbraSha)
- **Team:** Abdal Security Group
- **Company:** Nahaanbin CyberSecurity Company
- **Email:** Prof.Shafiei@Gmail.com
- **Repository:** https://github.com/ebrasha/abdal-4iproto-client-android

### Reporting License Violations

If you discover a fork, distribution, or commercial use that violates
these terms (such as removed attribution, reused branding, or unauthorized
trademark use), please report it via the contact above.
