# OpenCV Tour

Android app to create and follow virtual tours, using OpenCV to identify objects of interest.

Created by Daniel Piros and Thanh Vu as a Lafayette College Computer Science department research project.

OpenCV Tour is licensed under the [GNU General Public License, version 3.0](http://www.gnu.org/licenses/gpl-3.0.html). See [`COPYING`](COPYING) for more details.


## Running

This repository contains both an Android Studio project and an Eclipse ADT project. Either one should work, but Android Studio makes it much easier to handle dependencies, and Google says you should use it.

Download the OpenCV 3.0.0 android sdk, and extract it to the base folder of the repository. Ensure it's named "OpenCV-android-sdk"

Install OpenCV Manager 3.0.0 on your device:

`adb install OpenCV-3.0.0-android-sdk/apk/OpenCV_3.0.0_Manager_3.00_<platform>.apk`

where `<platform>` is the architecture of your device, obtained via `cat /proc/cpuinfo` in adb shell.

### Eclipse users

On Eclipse, you need to create library projects for recyclerview, android-support-v7-appcompat, android-support-design, and google-play-services_lib. See https://developer.android.com/tools/support-library/setup.html#add-library for information on how to do so. You will also need to either copy your Android SDK to the base folder of the repository (renaming it "AndroidSdk") or create a symbolic link to the SDK.

For android-support-design, you will need to change the target version to 22, and add "android-support-v7-appcompat" as an Android dependency.

For google-play-services_lib, follow the instructions at https://developers.google.com/android/guides/setup, copying the library to beside the repository's base directory.

Also add the OpenCV-android-sdk/sdk/java/ and android-advancedrecyclerview/library/src/main/ projects to your workspace.