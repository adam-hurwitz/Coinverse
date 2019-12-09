# Coinverse Open App
**Coinverse is the first audiocast app for cryptocurrency news, also including YouTube and text. Check it out on the [Play Store](https://play.google.com/store/apps/details?id=app.coinverse) or [subscribe for updates](https://mailchi.mp/c6912adf3367/coinverse)!**

[![Coinverse YouTube video](https://carpecoin-media-211903.firebaseapp.com/youtube-preview.png)](https://youtu.be/haXPolAruoc)
<div align="center">Coinverse YouTube video</div>

## Developers
### [App Setup Instructions](https://medium.com/coinverse/coinverse-open-app-set-up-7a9fdbd1ba46)
### Architecture & Libraries
#### Client - Kotlin
- Unidirectional Data Flow w/ Data Binding, ViewModels, LiveData, and Coroutines
- Navigation component
- PagedListAdapter with Room SQL library
- ExoPlayer for audiocasts in Foreground service
- Firebase: Firestore Db, Authentication, Analytics, Remote Config, Crashlytics
- MoPub native ads
- Interactive graph of price data
- YouTube data API
- Content quality scores based on user interaction
- Published with latest App Bundle format

#### Backend - Kotlin, Node.js
_Kotlin: Jar_
- Populates news content
- RxJava to manage Retrofit data streams
- Hosted on AppEngine with Firestore database
- Staging and production environments on Firebase
- Firestore security rules to manage access to data
- JUnit tests for avg. price calculation

_Node.js: Firebase Cloud Functions_
- Generates audiocast mp3s using Google's Text-to-Speech API
- Delete user data

## About
### Why crypto first?
![Why crypto first?](https://carpecoin-media-211903.firebaseapp.com/why-crypto-first.png)
### Price predictions, not useful info
![Price predictions, not useful info](https://carpecoin-media-211903.firebaseapp.com/price-predictions.png)

## Features
### Coinverse content includes
![Content strategy](https://carpecoin-media-211903.firebaseapp.com/content-strategy.png)
### Audiocasts
![Audiocasts](https://carpecoin-media-211903.firebaseapp.com/audiocasts.png)
### YouTube
![YouTube](https://carpecoin-media-211903.firebaseapp.com/youtube.png)
### Future
![YouTube](https://carpecoin-media-211903.firebaseapp.com/future.png)
