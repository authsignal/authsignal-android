# Authsignal Android

Check out our [official Android documentation](https://docs.authsignal.com/sdks/client/android).

## Installation

Ensure that you have `mavenCentral` listed in your project's buildscript repositories section:

```
buildscript {
  repositories {
    mavenCentral()
    ...
  }
}
```

Add the following to your app's build.gradle file:

#### Passkeys

```
implementation 'com.authsignal:authsignal-passkey-android:0.1.6'
```

#### Push auth

```
implementation 'com.authsignal:authsignal-push-android:0.2.6'
```

## Initialization

#### Passkeys

```kotlin
import com.authsignal.passkey.AuthsignalPasskey
...

val authsignalPasskey = AuthsignalPasskey("YOUR_TENANT_ID", "YOUR_REGION_BASE_URL")
```

#### Push auth

```kotlin
import com.authsignal.push.AuthsignalPush
...

val authsignalPush = AuthsignalPush("YOUR_TENANT_ID", "YOUR_REGION_BASE_URL")
```

You can find your tenant ID in the [Authsignal Portal](https://portal.authsignal.com/organisations/tenants/api).

You must specify the correct base URL for your tenant's region.

| Region      | Base URL                         |
| ----------- | -------------------------------- |
| US (Oregon) | https://api.authsignal.com/v1    |
| AU (Sydney) | https://au.api.authsignal.com/v1 |
| EU (Dublin) | https://eu.api.authsignal.com/v1 |

## Usage

### Passkeys

For more detailed info on how add passkeys to your app using Authsignal, check out our [official passkey documentation for Android](https://docs.authsignal.com/sdks/client/android#passkeys).

### Push auth

To see how to add push authentication to your app using Authsignal, see our [official push documentation for Android](https://docs.authsignal.com/sdks/client/android#push).
