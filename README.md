# Family Stock Dashboard

An Android Jetpack Compose app for families to track a shared Indian stock portfolio, discuss investment decisions, manage watchlist targets, import broker statements, and generate AI-assisted market summaries.

## Features

- Local passcode-based sign in and registration
- Family group creation and invite-code joining
- Shared portfolio holdings with value, gain/loss, and allocation chart
- CSV/XLSX broker statement import
- Family investment chat thread
- Stock search and research screen
- Shared watchlist with target-price alerts
- Gemini-powered portfolio and stock summaries, with local fallback analysis
- Light, dark, and system theme support

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Kotlin coroutines and StateFlow
- Retrofit, Moshi, and OkHttp
- Apache POI for spreadsheet imports

## Local Setup

1. Open the project in Android Studio.
2. Create a local `.env` file from `.env.example`.
3. Set `GEMINI_API_KEY` in `.env` if you want live Gemini summaries.
4. Set `STOCK_INDIAN_API_KEY` in `.env` if you want IndianAPI market data.
5. Build and run the app with the included Gradle wrapper.

Do not commit `.env`, signing keys, local Android SDK paths, generated build folders, or downloaded tooling archives.

## API Key Security

API keys in Android apps are not truly secret because APKs can be decompiled. The local `.env` setup keeps keys out of GitHub, which is required for public repositories, but a production app should call a small backend/proxy that stores provider keys server-side and rate-limits requests per user/device.

## Project Status

This app is currently being hardened from a prototype into a production-quality mobile app. The immediate priorities are security cleanup, Gemini API fixes, lifecycle-safe background sync, architecture cleanup, and meaningful tests.
