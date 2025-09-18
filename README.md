# eSIM Switcher

A simple Android application that allows users to quickly switch between installed eSIM profiles on their device.

## Features

- **Quick eSIM Profile Switching**: Reduces the number of clicks needed to switch between eSIM profiles
- **Profile Overview**: View all available eSIM profiles with carrier information
- **Active Profile Indication**: Clearly shows which profile is currently active
- **Simple Interface**: Clean, intuitive design optimized for tablets

## Requirements

- Android 9.0 (API level 28) or higher
- Device with eSIM support
- Appropriate system permissions for telephony management

## Permissions

The app requires the following permissions:
- `READ_PHONE_STATE`: To read eSIM profile information
- `MODIFY_PHONE_STATE`: To switch between eSIM profiles
- `ACCESS_NETWORK_STATE`: To check network connectivity

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Build and install on your eSIM-enabled Android device

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