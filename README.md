# OpenCV Tour

Android app to create and follow virtual tours, using OpenCV to identify objects of interest.

Created as a Lafayette College Computer Science department research project.


## Running

This repository contains both an Android Studio project and an Eclipse ADT project. Android Studio is recommended, but you can use whichever you're most comfortable with.

Download the OpenCV 3.0.0 android sdk, and extract it to the root folder of the repository. Ensure it's named "OpenCV-android-sdk"

Install OpenCV Manager 3.0.0 on your device:

`adb install OpenCV-3.0.0-android-sdk/apk/OpenCV_3.0.0_Manager_3.00_<platform>.apk`

where `<platform>` is the architecture of your device, obtained via `cat /proc/cpuinfo` in adb shell.

### Eclipse only

On Eclipse, you need to create library projects for recyclerview, android-support-v7-appcompat, and google-play-services_lib. See https://developer.android.com/tools/support-library/setup.html#add-library for information on how to do so. You will also need to either copy your Android SDK to the root folder of the repository (renaming it "AndroidSdk") or create a symbolic link to the SDK.

You will also need to add the OpenCV-android-sdk/sdk/java/ project to your workspace.