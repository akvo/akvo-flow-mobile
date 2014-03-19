Akvo FLOW Field Survey app 
Last update 19 March 2014

#ver 1.13.3

New Features & enhancements
---------------------------
* Add 'Quiche' language support (#75)

Bug fixes
---------
* Fix 'Clear' menu option within a survey response (#74)

#ver 1.13.2

New Features & enhancements
---------------------------

* Accurately compute survey duration (#69)
* Cleanup failed APK downloads (#71)

#ver 1.13.0

New features & enhancements
---------------------------

* Implement Double Entry functionality (#40)
* Handle app upgrade from within the app (#61)
* Improve reliability of data transfer to S3 (#56)
* Unused code and features clean up (#49 and #50)

Bug fixes
---------

* Images were always marked as 'Sent' (#55)
* Restrict the TEXT question length to 500 chars (#48)
* Add the proper Mime Type depending on the file (#64)

#ver 1.12.1

New features & enhancements
---------------------------
* Recompute survey langauges when upgrading from an old version (Issue #34)

Bug fixes
---------
* Fix memory leak in Dialogs (Issue #33)

#ver 1.12.0

New features & enhancements
---------------------------
* Data deletion option (partial or total) in Settings (#2)
* Remove unused assets and general cleanup (#4)
* Performance improvement in the communication between FLOW and Amazon S3 (#9)
* Remove duplicate images from DCIM directory in sd card (#11)
* Notify the user and close the app if no external storage is found (#21)
* Record survey duration (#28)
* Go back to Home Screen after survey submission (#30)

Bug fixes
---------
* GPS keeps working when the application is no longer active (#1)
* HttpUrlConnection converts request headers to lowercase (#13)
* File upload stalls at 99% (#15)
* Avoid recreating the list every 10 seconds in survey transmission history (#26)


#ver 1.11.1
* Change accuracy threshold of device GPS to 25 meters and stop checking for location after threshold is reached (#301)
* Check HTTP response's ETag to ensure an upload to Amazon S3 has been successful (#303)

#ver 1.11.0
* Update available survey languages on device to dynamic list that builds according to languages available in the survey xml for all surveys currently assigned to the device (#259)

#ver 1.10.6  
* Fix bug where device ID and other fields would accept line breaks and tab characters, which could generate broken data.txt files (#255)
* Always include serverBase from survey.properties in server selection. Makes instance builds independent of res/values/arrays.xml

#ver 1.10.5  
* Prevent display rotation from forgetting a taken photo (#118)
* Fix survey status partially-successful icon bug (#183)

#ver 1.10.4
* Notify user when SD card nearing capacity (#208)

#ver 1.10.3	
* Report OS version to server in beacon call (#204)

#ver 1.10.2	
* Automatically shrink photos (#188)
* Warn about large media files (#182)
* report IMEI to server (#xxx)

#ver 1.10.1


#ver 1.9.36
8 July 2012, co

Overview
----
This release contains improvements to the FLOW Field Survey application related to the storage of files on the device, a new keystore for app generation for easier installation of updates, and a new feature to allow users to scan barcodes from within a survey on the application.

New features & enhancements
----
### New feature: Barcode question type in surveys
A new question type that allows users to scan a barcode from inside their FLOW survey while collecting data in the field. The user selects the question type from the Survey Manager on the Dashboard when creating a survey, and then on the device this question prompts the user to "Scan Barcode," which calls an external barcode scanning app, and then populates the text field with the barcode number. REQUIRES an external barcode scanning app to be installed on the device. Barcode scanning apps that have been tested for this feature are: ZXing and QuickMark.

GitHub issues:
[17](https://github.com/akvo/akvo-flow/issues/17), [59](https://github.com/akvo/akvo-flow/issues/59)

### Enhancement: Enable users to store large numbers of surveys and photos on device SD card
Users can now store very large numbers of survey zip files and photos on their device's SD card. This is necessary when data collectors are going for long periods offline, where surveys must be stored on the devices for later upload. Since there is a limit for the number of files that can be stored in each directory independent of SD card storage capacity, this is achieved using a new file structure that distributes the files into multiple directories, instead of just storing them in the SD card root directory as before.

GitHub issue:
[16](https://https://github.com/akvo/akvo-flow/issues/16)

### Enhancement: New keystore for apk generation to eliminate signature conflicts
Created and distributed a release keystore along with instructions for use so that there is a single signature for the Field Survey application. This solved the problem from previous versions where, when multiple keystores were used to generate the app, users would face a signature conflict when trying to update the app on the device that forced them to uninstall the existing app before installing an update.

GitHub issue:
[21](https://https://github.com/akvo/akvo-flow/issues/21)


Bug fixes
----
### Email address of user not displayed correctly in user management

GitHub issue:
[51](https://https://github.com/akvo/akvo-flow/issues/51)


Known Issues
----
### Compatibility issues with Android OS version 2.1

Barcode Scanning
External barcode scanning apps (QuickMark, ZXing) crash or freeze in Android OS 2.1. As a result, we have not made this feature available for the apk running on 2.1. You can still enter barcodes manually into surveys when a barcode question type is present.

GPS Status App
Cannot reliably launch GPS Status app from inside the Field Survey application running on Android OS 2.1 (Settings > GPS Status).

#ver 1.9.35

* Gray out "Scan Barcode" button in apk running on OS 2.1 (#59)
* Fixed bug where email address of user not displayed correctly in user management
* Create initial test scripts for the apk (#37)
* Modify apk signature to eliminate signature conflicts that forced some users to uninstall before updating application (#21)
* Add ability to scan barcodes from app (#17)
* Fixed a defect where device users that exceeded the SD card file directory limits were losing data when directories were full (16)
*
