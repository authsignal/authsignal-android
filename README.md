<img width="1070" alt="Authsignal" src="https://raw.githubusercontent.com/authsignal/authsignal-android/main/.github/images/authsignal.png">

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

```
implementation 'com.authsignal:authsignal-android:2.2.12'
```

## Initialization

```kotlin
import com.authsignal.Authsignal
...

val authsignal = Authsignal("YOUR_TENANT_ID", "YOUR_REGION_BASE_URL")
```

You can find your tenant ID in the [Authsignal Portal](https://portal.authsignal.com/organisations/tenants/api).

You must specify the correct base URL for your tenant's region.

| Region      | Base URL                         |
| ----------- | -------------------------------- |
| US (Oregon) | https://api.authsignal.com/v1    |
| AU (Sydney) | https://au.api.authsignal.com/v1 |
| EU (Dublin) | https://eu.api.authsignal.com/v1 |

## Usage

For more detailed information on how to add passkeys and other MFA and passwordless authentication methods to your app using Authsignal, refer to our [Mobile SDK documentation](https://docs.authsignal.com/sdks/client/mobile).
