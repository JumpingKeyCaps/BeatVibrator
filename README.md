 # 🎵 BeatVibrator

  [![Platform](https://img.shields.io/badge/platform-Android-blue)](#)
  [![Compose](https://img.shields.io/badge/Jetpack_Compose-enabled-orange)](#)
  [![Haptics](https://img.shields.io/badge/Haptics-LRA_Advanced-important)](#)

  **BeatVibrator** is an experimental Android app that transforms an MP3 file into a **synchronized tactile experience**. It analyzes the audio signal, extracts coherent haptic patterns (low frequencies, transients, energy), and uses the **VibratorManager** to trigger vibrations precisely in sync with the music.

  ---

  ## 🚀 Features

  - 🎧 **MP3 Import** via Storage Access Framework (SAF)
  - 🔎 **On-device DSP analysis**: FFT, envelope, RMS, transient detection
  - 📳 **Pre-computed** `VibrationPattern` optimized for LRA motors
  - 🌈 **Pseudo-3D fractal visualization** driven by the audio signal
  - 💿 Display of **audio metadata** (title, artist, duration…)
  - ⏯️ **Basic playback controls** (play, pause, stop)
  - 🌌 **Real-time haptic rendering** synced with playback

  ---

  ## ⚙️ Tech Stack

  | Technology             | Role                                                 |
  |------------------------|------------------------------------------------------|
  | Kotlin                 | Main language, clean MVVM architecture              |
  | Jetpack Compose        | Modern, reactive, immersive UI                      |
  | ExoPlayer + AudioProcessor | PCM extraction + playback control                |
  | Custom DSP (FFT, RMS, IIR) | Time/frequency domain audio analysis            |
  | VibratorManager        | Fine-grained haptic pattern handling                |
  | Coroutine Flow         | Async processing and playback-haptics sync         |
  | Hilt (optional)        | Modular dependency injection                        |

  ---

  ## 📐 Pipeline: MP3 → Haptic Pattern (precomputed)

  A step-by-step breakdown of the signal processing path:

  1. **Audio loading** using `ExoPlayer` with a custom `AudioProcessor`  
     → Raw 16-bit PCM audio extraction (mono/stereo)

  2. **DSP pre-processing**:
     - Short → Float conversion [-1.0f, +1.0f]
     - Optional stereo averaging
     - 2nd order Butterworth low-pass filter (100–250 Hz)
     - RMS over 10–30ms windows
     - Onset detection (RMS delta > dynamic threshold)

  3. **Haptic mapping**:
     - Normalized RMS → amplitude (0–255) using exponential mapping
     - Minimum threshold to skip weak vibrations
     - Additional strong impulses on detected onsets

  4. **Pattern generation**:
     - List of `HapticEvent(timestampMs, durationMs, amplitude)`
     - Merge consecutive similar segments for optimization
     - In-memory storage for synchronized playback

  5. **Synchronized playback**:
     - Dedicated coroutine for triggering vibrations during playback
     - Latency compensation (LRA offset ~20–50ms)
     - Sequential calls to `VibrationEffect` in real-time

  ---

  ## 🔄 User Flow

  1. User taps the **FAB** to select an MP3 file.
  2. Audio is analyzed in the background → generates a `VibrationPattern`.
  3. User hits **Play**.
  4. **Music plays**, while **LRA vibrations** are triggered in parallel.
  5. The UI displays **metadata** and a **responsive visualization**.

  ---

  ## 📅 Roadmap (MVP)

  - [ ] MP3 import (SAF)
  - [ ] PCM extraction via AudioProcessor
  - [ ] RMS + low-pass filter on PCM buffer
  - [ ] Precompute `HapticEvents`
  - [ ] Dynamic visualization driven by RMS/onsets
  - [ ] Immersive UI (fullscreen, pink/red gradient)
  - [ ] Playback ↔ vibration synchronization with latency offset
  - [ ] Advanced `VibratorManager` integration (API ≥ S)
  - [ ] Pattern display & editing (dev/debug mode)
  - [ ] Settings: sensitivity, intensity, mapping type

  ---

  ## 🧪 Limitations & Device Requirements

  - Works **only** on Android devices with a **high-quality LRA motor** (API 31+ recommended).
  - Supports **one MP3 file at a time** — no playlist or queue.
  - Haptic perception is **highly device-dependent**, influenced by chassis design and user sensitivity.
  - Sync precision is limited by **hardware vibration latency** (typically 30–50ms).

  ---

  ### Differences Between LRA and ERM Motors

  **ERM motors** (Eccentric Rotating Mass) produce vibration by spinning an off-center weight. They’re cheap, durable, and easy to drive but have slow response times (rise/fall > 30ms), making them unsuitable for precise or rhythm-synced feedback (e.g., music or high-resolution haptics).

  **LRA motors** (Linear Resonant Actuators) use linear oscillation around a fixed resonance frequency (usually 175–235 Hz). They offer faster response, higher precision, and can be driven efficiently using **fixed-frequency PWM with amplitude modulation**. This makes them suitable for rich or rhythmic haptic patterns.

  **Limitations:**
  - LRA motors often require a **dedicated driver IC** (e.g., TI DRV2605).
  - Their **effective bandwidth is narrow**, centered on the resonance frequency.
  - They **can’t reproduce complex audio signals** (like full music), but are ideal for simulating beats and rhythmic pulses.

  > 🔧 In short: ERM for basic cheap buzz, LRA for accurate effects — but with hardware constraints.

  ---

  ## 🧠 Motivation & Vision

  BeatVibrator is a personal research project exploring:

  - **Tactile musicality** and translating rhythm into physical sensation
  - The advanced **haptic capabilities** of LRA motors on Android
  - The connection between **frequency-domain analysis** and sensory design
  - New **sensory languages** for the deaf and synesthetic experiences

  ---

  ## 💡 Original Idea

  A personal research project to explore **tactile musicality**, the haptic limits of Android devices, and the bridges between sound & vibration.
