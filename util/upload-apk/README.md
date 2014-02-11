## Handling a new APK version

### How apks are stored on S3
1. All apks are stored in a folder apk, in the S3 bucket akvoflow.
2. For each instance, we have a folder under /apk, with the name equal to the instance application property, from appengine-web.xml
3. apks have the following name format: fieldsurvey-x.y.z.apk, with x.y.z the version number

### How apks are build
1. The script `flowreleases.sh`, which lives in the `/survey` project folder, builds and uploads the apks to S3.
2. The script does not need arguments
3. The script takes the version number from the `Android:versionName` property in the `AndroidManifest.xml` file
4. The apks are build and stored in the folder `survey/builds`

### How apks are uploaded to S3
4. The script uses a jar file `uploadS3.jar`, which takes four arguments:
    * S3 access key
    * S3 secret key
    * instanceId
    * path to new apk
5. uploadS3 takes the apk and uploads it to s3, using the provided credentials. It also sets the content type and public access.

### How instances are notified of a new apk
1. Information on available apks is stored in the DeviceApplication kind in the backend
2. DeviceApplication has four properties: `deviceType`,  `appCode`, `version`, `fileName`. For a FLOW apk, these properties are:
    * deviceType = androidPhone
    * appCode = fieldSurvey
    * version = x.y.z
    * filename = http://akvoflow.s3.amazonaws.com/apk/INSTANCE_ID/fieldsurvey-xx.yy.zz.apk

3. A new DeviceApplication record can be made by an API call:
    http://host.akvoflow.org/actions&action=newApkVersion&version=x.y.z
4. The API call is secured and will only work if you are logged in as a FLOW admin user.     
5. This needs to be done for each instance. In this way, the update can be managed per instance.

### Creating DeviceApplication record for all instances
1. If we want to set all instances to the latest version, we can use a script `apkUpdateInstances.sh`, which should be run from the `surveys` folder.
2. The `apkUpdateInstances.sh` takes its apk version information directly from AndroidManifest.xml.

### Stopping an instance from sending new versions to devices
1. If we want to stop an instance to send new versions to devices, we can set the property `autoUpdateApk` in appengine-web.xml to `false`, and redeploy the instance.

### Proces for a new apk
1. release code
2. run flowreleases.sh
3. run the deviceApplication api for each instance that you want to update, or run the `apkUpdateInstances.sh` script.