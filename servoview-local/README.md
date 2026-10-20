# Servo Engine AAR — Placeholder

Place `servo-aarch64-android.aar` in this directory.

## How to obtain:
1. Go to https://github.com/servo/servo/releases
2. Download the latest `servo-aarch64-android.aar` artifact
3. Place it here: `servoview-local/servo-aarch64-android.aar`

## Build from source (alternative):
```bash
# On Linux/macOS (WSL works):
export ANDROID_SDK_ROOT=/path/to/android-sdk
export ANDROID_NDK_ROOT=/path/to/android-ndk
./mach build --android --release
# AAR will be at: target/android/release/servo-aarch64-android.aar
```

## Architecture note:
- `aarch64` = ARM64 (arm64-v8a) — physical devices
- `i686` = x86 — emulator only (separate build)
