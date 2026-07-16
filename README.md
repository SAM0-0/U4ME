<div align="center">
  <img src="app/src/main/res/drawable/ic_u4me_logo.jpg" alt="U4Me Logo" width="150" height="150" style="border-radius: 20px;">
  <h1>U4Me</h1>
  <p><b>A sleek, open-source Android client for Mahindra EV Owners to securely check their true State of Health (SOH).</b></p>
  
  [![GitHub Repo stars](https://img.shields.io/github/stars/SAM0-0/U4ME?style=social)](https://github.com/SAM0-0/U4ME)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android)](https://www.android.com/)
  [![Web](https://img.shields.io/badge/Platform-Web-4285F4.svg?logo=googlechrome)](https://bev-soh.edgeone.dev/)
</div>

<br/>

## 🔋 What is U4Me?
This tiny, ultra-premium app allows you to instantly check your EV's **State of Health (SOH)** by interfacing directly with Mahindra's me4u infrastructure. We built this to give EV owners full transparency into their battery life without any unnecessary bloat or tracking.

> **⚠️ Note:** We don't know how long this infrastructure loophole will remain active, but while it does, it is a fantastic tool to track your battery's actual health!

## ✨ Features
- **Instant SOH Sync:** Secure OTP-based login to fetch real-time battery degradation stats.
- **Community Like Button:** We added a "Like" button directly in the app to help us track how many active users we have! If you use the app and find it helpful, **please make sure to hit the Like button** so we can see how large our community is growing!
- **100% Open Source:** No hidden trackers, no analytics, no ads. Just pure, functional code.

## 🛡️ Trust & Security
We know that putting your phone number into a random APK or website is scary. 
**That is exactly why U4Me is 100% open source.** 

The repository is cleanly split so you can easily audit it:
- **`app/`**: Contains the source code for the Android application.
- **`Website-Source-Code/`**: Contains ONLY the files used by the web version.

You never have to blindly trust our release files. You can inspect every single line of code in this repository, verify exactly where the network requests are going, and even compile the app yourself directly from the source code using Android Studio!

### How the "Like" Button Works
When you tap the Like button, it makes a simple, anonymous request to an independent Cloudflare worker just to increment and fetch the total number of community likes. It does NOT send your phone number, VIN, or any personal data whatsoever!

## 📥 Installation
1. Navigate to the [Releases page](https://github.com/SAM0-0/U4ME/releases).
2. Download the latest `app-release.apk` file.
4. Launch the app, login via OTP, and check your SOH!

## 🌐 Web Version (No Download Required)
Don't want to install an APK? No problem! You can use our fully trusted, cloud-hosted web version here:
👉 **[bev-soh.edgeone.dev](https://bev-soh.edgeone.dev/)**

The web version runs the exact same HTML/JS code you see in the `Website-Source-Code/index.html` file in this repository. 

### What is the Cloudflare Worker & CORS?
If you've ever tried to run the HTML file directly on your computer, you might have hit a "CORS error". Browsers block cross-origin requests to Mahindra's servers for security reasons. To fix this seamlessly, the website uses a **Cloudflare Worker** as a secure backend proxy to route traffic directly to the Mahindra me4u infrastructure and return the responses properly. 

For full transparency, the exact Cloudflare Worker source code running on the proxy is included in `Website-Source-Code/cloudflare_worker_proxy.txt`. It simply forwards your requests and adds CORS headers. Nothing is logged or saved!


## 🤝 Feedback & Issues
We want to make this app as polished as possible! 
- Did you find a bug?
- Do you have an idea for a cool new feature?
- Do you just want to give feedback on the UI?

Please **[File an Issue](https://github.com/SAM0-0/U4ME/issues)**! We actively read all feedback and would love to hear your thoughts. If you enjoy the app, don't forget to leave a ⭐ on the repository!

---
<div align="center">
  <i>Built with ❤️ for the EV Community</i>
</div>
