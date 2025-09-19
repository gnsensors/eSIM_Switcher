# eSIM Switcher

A simple Android application that allows users to quickly switch between installed eSIM profiles on their device.

## Features

- **Quick eSIM Profile Switching**: Reduces the number of clicks needed to switch between eSIM profiles
- **Profile Overview**: View all available eSIM profiles with carrier information
- **Active Profile Indication**: Clearly shows which profile is currently active
- **Simple Interface**: Clean, intuitive design optimized for tablets

## Requirements

### Device Requirements
- Android 9.0 (API level 28) or higher
- Device with eSIM support
- Appropriate system permissions for telephony management

### Development Requirements
- Android Studio Arctic Fox (2020.3.1) or later
- Kotlin 1.9.10
- Android Gradle Plugin 8.12.3
- Gradle 8.0+
- JDK 8 or higher

## Permissions

The app requires the following permissions:
- `READ_PHONE_STATE`: To read eSIM profile information
- `MODIFY_PHONE_STATE`: To switch between eSIM profiles
- `ACCESS_NETWORK_STATE`: To check network connectivity

## Dependencies

The app uses the following key dependencies:
- AndroidX Core KTX 1.12.0
- AndroidX AppCompat 1.6.1
- Material Design Components 1.10.0
- AndroidX ConstraintLayout 2.1.4
- AndroidX Lifecycle (ViewModel & LiveData) 2.7.0
- AndroidX RecyclerView 1.3.2

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/gnsensors/eSIM_Switcher.git
   ```
2. Open the project in Android Studio
3. Sync the project to download dependencies
4. Build and install on your eSIM-enabled Android device

## Usage

1. Launch the app
2. Grant the required permissions when prompted
3. View available eSIM profiles
4. Tap "Switch Profile" on the desired profile
5. Confirm the switch in the system dialog

## Technical Notes

- The app uses Android's SubscriptionManager APIs for eSIM management
- Profile switching requires Android 9.0+ for full functionality
- System-level confirmation dialogs are displayed for security

## License

This project is open source. Please check the license file for details.