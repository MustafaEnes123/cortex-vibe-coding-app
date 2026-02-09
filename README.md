# 🧠 Cortex: AI-Powered Second Brain

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Gemini](https://img.shields.io/badge/AI-Gemini%20Pro-8E75B2?style=for-the-badge&logo=google&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Backend-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

> **Crafted with Vibe Coding.** 🚀
> Cortex is not just a bookmark manager; it's your AI-powered second brain designed to declutter your digital life, summarize chaos, and organize knowledge.

---

## ✨ Key Features

* **🤖 Gemini AI Integration:** Instantly summarize articles, videos, or web pages with a single tap. Chat with your bookmarks to extract key insights.
* **🔗 Smart Sharing (Intent Receiver):** seamless integration with the Android ecosystem. Share links directly to Cortex from Chrome, Twitter, YouTube, or any other app.
* **📂 Hybrid Storage:** Your data is safe and accessible everywhere. Uses **Room Database** for offline access and **Firebase Firestore** for cloud sync.
* **⚡ Jetpack Compose UI:** Built with Google's modern UI toolkit. Features a sleek, fluid, and Material 3 compliant interface.
* **🔍 Rich Link Previews:** Automatically extracts metadata (titles, images, descriptions) using Jsoup for a visual browsing experience.

---

## 🛠️ Tech Stack

This project is built using modern Android development standards:

* **Language:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel) & Clean Architecture principles
* **AI Model:** Google Gemini Pro (`google.ai.generativeai`)
* **Local DB:** Room Database
* **Remote DB & Auth:** Firebase Firestore & Authentication
* **Network:** Retrofit & Jsoup (for parsing)
* **Image Loading:** Coil
* **Async:** Coroutines & Flow

---

## 🚀 Getting Started

Follow these steps to run the project on your local machine:

1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/mustafaenes123/cortex-vibe-coding-app.git](https://github.com/mustafaenes123/cortex-vibe-coding-app.git)
    ```

2.  **Configure API Keys:**
    This project uses Google Gemini. Add your API key to your `local.properties` file:
    ```properties
    GEMINI_API_KEY="AIzaSyD......"
    ```

3.  **Firebase Setup:**
    * Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    * Download your `google-services.json`.
    * Place the file inside the `app/` directory.

4.  **Build and Run:**
    Open the project in Android Studio and click the `Run` button.

---

## ⚠️ Important Note

This repository does **not** include the `google-services.json` file for security reasons. You must provide your own Firebase configuration to enable cloud features.

## 🤝 Contributing

Contributions are always welcome! If you have a major change in mind, please open an issue first to discuss it.

## 📄 License

[MIT](LICENSE) © 2026 Mustafa Enes Kayacı
