
 <p align="center">
  <img src="screenshots/logoappclear.png" alt="Logo" width="250" height="250">
</p>
 
 # BeatVibrator

  ![Status](https://img.shields.io/badge/status-WIP-red)
  ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=android&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?logo=androidstudio&logoColor=white)
  [![Haptics](https://img.shields.io/badge/Haptics-LRA_Advanced-important)](#)


  **BeatVibrator** is an experimental Android app that transforms an MP3 file into a **synchronized tactile experience**. It analyzes the audio signal, extracts coherent haptic patterns (low frequencies, transients, energy), and uses the **VibratorManager** to trigger vibrations precisely in sync with the music.

  ---

  ## 🚀 Features

  -  **MP3 Import** via Storage Access Framework (SAF)
  -  **On-device DSP analysis**: FFT, envelope, RMS, transient detection
  -  **Pre-computed** `VibrationPattern` optimized for LRA motors
  -  **Efficient MP3 Decoding**: MP3 → PCM via `MediaExtractor` / `MediaCodec`
  -  **Full DSP Analysis**: Low-pass filter, RMS, FFT, onsets, BPM detection
  -  **Smart Post-processing**: Perceptual compression & merging of haptic pulses
  -  **Precise Synchronization**: Time-aligned vibration events (LRA)
  -  **Modern Architecture**: Clean Architecture with Hilt injection
  -  **Live State Tracking**: `StateFlow` monitoring for each stage

  ---

   ## 🔄 User Flow

  1. User taps the **FAB** to select an MP3 file.
  2. Audio is analyzed in the background → generates a `VibrationPattern`.
  3. User hits **Play**.
  4. **Music plays**, while **LRA vibrations** are triggered in parallel.
  5. The UI displays **metadata** and a **responsive visualization**.

  ---

  ## ⚙️ Tech Stack

  | Technology             | Role                                                 |
  |------------------------|------------------------------------------------------|
  | Kotlin                 | Main language, clean MVVM architecture              |
  | Jetpack Compose        | Modern, reactive, immersive UI                      |
  | MediaCodec + MediaExtractor | PCM extraction (offline)                        |
  | Custom DSP (FFT, RMS, IIR) | Time/frequency domain audio analysis            |
  | Post-Processing        | Cleaning, BPM synch,...                             |
  | VibratorManager        | Fine-grained haptic pattern handling                |
  | Exoplayer 3            | Audio Mp3 playback                                  |
  | Coroutine Flow         | Async processing and playback-haptics sync          |
  | Hilt (optional)        | Modular dependency injection                        |

  ---

 ## 📐 Pipeline: MP3 → Haptic Pattern (precomputed)

A step-by-step breakdown of the signal processing path:

1. **Audio decoding** using `MediaCodec` (offline / chunk)
   
   → Direct MP3 → PCM (16-bit signed, mono/stereo) without playback  
   *(Used only for analysis and vibration generation, not audio output)*

   - Chunk-based processing : Read and process the audio data in small buffers (or chunks) rather than loading the entire MP3 file into memory all at once.

     This strategy is crucial for preventing Out Of Memory (OOM) 

3. **DSP processing**:
   - Short → Float conversion [-1.0f, +1.0f]
   - Optional stereo averaging
   - 2nd order Butterworth low-pass filter (100–200 Hz)
   - RMS over 10–30ms windows
   - Onset detection (RMS delta > dynamic threshold)

4. **Post-processing refinement**:
   - Normalize RMS → amplitude (0–255) using exponential mapping
   - Skip low-amplitude segments (threshold gating)
   - Inject stronger impulses on detected onsets
   - Remove ultra-short or weak pulses
   - Merge clustered onsets within a short window
   - Compress amplitudes (soft perceptual scaling)
   - Dynamically adjust pulse duration based on energy
   - Optional beat quantization (snap to BPM grid)
   - Clamp & sort final timeline

5. **Pattern generation**:
   - Build list of `HapticEvent(timestampMs, durationMs, amplitude)`
   - Merge consecutive similar segments for optimization
   - In-memory storage for synchronized playback

6. **Synchronized playback**:
   - **Audio playback** using `ExoPlayer`(media3)
   - Dedicated coroutine for triggering vibrations during playback
   - Latency compensation (LRA offset ~20–50ms)
   - Sequential calls to `VibrationEffect` in real-time

  ---
  
 ### Signal Processing Pipeline
 ```
      +---------------------------------------------+
      | 1. MP3 Selection via SAF                    |
      +--------------------+------------------------+
                           |
                           v
      +---------------------------------------------+
      | 2. MP3 Decoding → PCM                       |
      |    - MediaExtractor + MediaCodec            |
      |    - PCM Buffer/chunk (FloatArray)          |
      +--------------------+------------------------+
                           |
                           v
      +---------------------------------------------+
      | 3. Full DSP Analysis                        |
      |                                             |
      |  3.1. Butterworth Low-pass Filter (200Hz)   |
      |  3.2. RMS Calculation (sliding windows,...) |
      |  3.3. BPM Detection (on RMS signal)         |
      |  3.4. FFT Spectrogram (FFT size, hop)       |
      |  3.5. Onset Detection (peak picking, ...)   |
      +--------------------+------------------------+
                           |
                           v
      +---------------------------------------------+
      | 4. Postprocessing                           |
      |    - Cleaning / Compression                 |
      |    - Non-linear Mapping                     |
      |    - Framing synced to BPM                  |
      |    - Intensity/duration adjustment          |
      +--------------------+------------------------+
                           |
                           v
      +---------------------------------------------+
      | 5. Haptic Pattern Generation                |
      |    - Precise timing (vibrations)            |
      |    - Intensities and durations              |
      +--------------------+------------------------+
                           |
                           v
      +---------------------------------------------+
      | 6. READY State & Playback Ready             |
      |    - UI / Vibrations enabled                |
      +---------------------------------------------+
 ```

  ---

## 🧠 Custom DSP Components

- `FFT`: Radix-2, pure Kotlin, with Hamming window + overlap
- `ButterworthFilter`: Biquad 2nd-order low-pass with internal state
- `OnsetDetector`: Adaptive threshold peak picking on spectral flux
- `RmsCalculator`: Sliding window RMS with optional normalization
- `BpmDetector`: Autocorrelation-based BPM estimator on RMS signal

---

## ⚙️ Technical Optimizations

 ### Performance
  - Efficient buffer use (chunked + throttled)
  - Codec timeout management
  - 50% overlap on RMS/FFT for smoother analysis
  - Early format validation to skip unsupported inputs
 
 ### Memory
  - Systematic cleanup of MediaCodec / Extractor
  - Block-based decoding to avoid memory peaks
  - Filter internal state reset between runs
 
 ### DSP Enhancements
  - Perceptual mapping: intensity^0.6
  - Temporal merge of close onsets (≤120ms)
  - BPM-based rhythmic quantization (optional)
  - Adaptive peak picking for transient detection

  ---

 ## 🧪 Limitations & Device Requirements

  - **Device Compatibility**: Works only on Android devices equipped with a high-quality LRA (Linear Resonant Actuator) motor (API 31+ recommended). Haptic perception is highly device-dependent, influenced by chassis design and user sensitivity.
 
  - **Audio File Support**: Supports only one MP3 file at a time—no playlist or queue.
 
  - **Haptic Sync Precision**: Sync precision is limited by hardware vibration latency (typically 30–50ms for LRA). For instance, a Galaxy S8 (2017) might have an LRA latency of around 50ms, whereas a recent Pixel device could achieve closer to 20ms. This latency is a critical factor for the fidelity of the haptic experience.
  
  - **Difficulty in Vibrator Identification**: It's extremely challenging to know the exact type of vibrator embedded in a phone and its precise technical specifications (like latency). Manufacturers generally provide very little detailed information on this.
     This knowledge can often only be gained through deduction, for example, by observing if the device supports amplitude control via `VibrationEffect.setAmplitude()` or, more recently, by using `VibrationEffect.setLraCustomEffect()` and the `VibrationEffect.getLraType()` method available from API 34, which can offer clues about the type of vibrator (ERM or LRA) and others capabilities.

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

  ## 📅 Roadmap (MVP)
  
  - [ ] MP3 import (SAF)
  - [x] MP3 extraction (Mp3-> Raw PCM)
  - [x] RMS + low-pass filter on PCM buffer
  - [x] Post-processing (amplitude mapping, cleanup, quantization)
  - [ ] Dynamic visualization driven by RMS/onsets
  - [ ] Playback ↔ vibration synchronization with latency offset
  - [ ] Advanced `VibratorManager` integration (API ≥ S)
  - [ ] debug: sensitivity, intensity, mapping type

  ---

 ## 📋 Requirements
  
  - Android SDK: 24+
  - Kotlin: 2.0.0+
  - Dagger Hilt: for DI
  - Coroutines: for async processing
  - Media3 Exoplayer : 1.7.1+ (K2 compatibility)

 ### Permissions
 
  ```
 <uses-permission android:name="android.permission.VIBRATE" />
 <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
 <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

  ```
 ---
 ### 🔮 Future Extensions

  - Real-time playback sync (ExoPlayer + Vibrator)
  - Additional formats (WAV, AAC, FLAC)
  - Visual feedback (onsets, spectrograms)
  - Genre-specific presets (EDM, Rock, Classical)
  - Haptic calibration for different phone motors
  - Import/export of haptic patterns
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

  ---
