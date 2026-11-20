# LALLA – Your Personal AI Companion

<p align="center">
  <b>A private, intelligent, multilingual Android AI companion built with Clean Architecture, Jetpack Compose, Room, and Hilt.</b>
</p>

---

## 🌟 Key Features (Phase 1)

- 🗣️ **Multilingual Natural Conversation**: Automatically detects and mirrors the user's language across **Hindi (Devanagari)**, **English**, and **Hinglish**.
- 🧠 **Persistent Long-Term Memory**: Automatically extracts and saves personal facts, preferences, and details into a local Room database, naturally referencing them in future conversations.
- ⚡ **Dual AI Engine Architecture (`AiEngine`)**:
  - **FreeLLMAPI / Cloud Mode (Default)**: Connects to [FreeLLMAPI](https://github.com/tashfeenahmed/freellmapi), Groq, or OpenRouter via OpenAI-compatible Server-Sent Events (SSE) streaming.
  - **Local Ollama Mode**: 100% private, on-device PC execution with `qwen2.5:3b` or `qwen2.5:7b`.
  - **Demo Mock Mode**: Instant on-device simulated responses with zero backend servers required.
- 🎨 **Modern Jetpack Compose UI**:
  - Real-time token-by-token streaming chat bubbles.
  - Dedicated Personal Memory screen with search, manual memory creation, and swipe/delete.
  - Runtime Settings screen to switch providers, endpoints, models, and temperature without restarting.

---

## 🏗️ Architecture

LALLA follows **Clean Architecture** with **MVVM** and unidirectional data flow:

```
app/src/main/kotlin/app/lalla/
├── domain/                  # Pure Kotlin business rules
│   ├── model/               # Message, Conversation, Memory, AiPrompt, AiResponse
│   ├── repository/          # Repository interfaces (ConversationRepository, MemoryRepository, AiRepository)
│   └── usecase/             # SendMessageUseCase, LoadConversationUseCase, Memory Use Cases
│
├── data/                    # Data sources & implementations
│   ├── ai/                  # AiEngine interface, RoutingAiEngine, OllamaEngine, OpenAiCompatibleEngine
│   ├── local/db/            # Room database, Entities, DAOs (MessageDao, ConversationDao, MemoryDao)
│   └── repository/          # Repository implementations
│
├── di/                      # Hilt dependency injection modules (DatabaseModule, AiModule, RepositoryModule)
│
└── ui/                      # Presentation layer (Jetpack Compose Material 3)
    ├── chat/                # ChatScreen, ChatViewModel
    ├── memory/              # MemoryScreen, MemoryViewModel
    ├── settings/            # SettingsScreen, SettingsViewModel
    └── navigation/          # LallaNavHost
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer
- **JDK 17**
- Physical Android phone with USB Debugging enabled (Android 8.0 / API 26+) or Android Emulator

### Option A: Running with FreeLLMAPI (Fast Cloud Models)
1. Start [FreeLLMAPI](https://github.com/tashfeenahmed/freellmapi) on your PC (runs by default on `http://localhost:3000`).
2. Connect your phone via USB and run in PowerShell:
   ```powershell
   adb reverse tcp:3000 tcp:3000
   ```
3. Open LALLA Settings:
   - Select **FreeLLM / Cloud**
   - URL: `http://localhost:3000/v1`
   - Model: `gemini-1.5-flash` or `llama-3.3-70b-versatile`

### Option B: Running with Local Ollama (100% Offline & Private)
1. Install [Ollama](https://ollama.com) on Windows and pull the model:
   ```powershell
   ollama pull qwen2.5:3b
   ```
2. Connect your phone via USB and reverse the port:
   ```powershell
   adb reverse tcp:11434 tcp:11434
   ```
3. Open LALLA Settings:
   - Select **Local Ollama**
   - URL: `http://localhost:11434`
   - Model: `qwen2.5:3b`

---

## 🛣️ Phased Roadmap

- [x] **Phase 1: Foundation & Core Chat** (Text chat, streaming tokens, Room DB, persistent memory, dual FreeLLM/Ollama routing)
- [ ] **Phase 2: Tasks & Alarms** (AlarmManager exact alarms, WorkManager reminders, system context injection)
- [ ] **Phase 3: Online Voice** (Android SpeechRecognizer STT, Android TextToSpeech TTS)
- [ ] **Phase 4: Offline Voice & Privacy** (sherpa-onnx offline STT, Piper TTS, SQLCipher database encryption)
- [ ] **Phase 5: Ambient Mode & Wake Word** (Always-listening "Hey LALLA" activation)
