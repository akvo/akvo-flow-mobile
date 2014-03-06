## Handling a new APK version

### How apks are stored on S3
1. All apks are stored in a folder apk, in the S3 bucket akvoflow.
2. For each instance, we have a folder under /apk, with the name equal to the instance application property, from appengine-web.xml
3. apks have the following name format: fieldsurvey-x.y.z.apk, with x.y.z the version number

### How apks are build
1. The script `flowreleases.sh`, which lives in the `/survey` project folder, builds and uploads the apks to S3.
2. The script does not need arguments, although it requires the following environment variables:
   * FLOW_DEPLOY_JAR=/path/to/deploy.jar
   * FLOW_SERVER_CONFIG=/path/to/akvo-flow-server-config
   * FLOW_S3_ACCESS_KEY=your_S3_access_key
   * FLOW_S3_SECRET_KEY=your_S3_secret_key
   * FLOW_GAE_USERNAME=google_username
   * FLOW_GAE_PASSWORD=google_password
    
   If any of those env vars is not set, the script will not run, displaying an error to request the missing variables.

3. The script takes the version number from the `Android:versionName` property in the `AndroidManifest.xml` file
4. The apks are build and stored in the folder `survey/builds`

### How apks are uploaded to S3
4. The script uses a jar file `deploy.jar`, which takes seven arguments:
   * accessKey - S3 access key
   * secretKey - S3 secret key
   * instanceId - name of the instance,
   * apkPath - the local path to the APK file to be
   * version - APK version name
   * username - Google Account username
   * password - Google Account password

5. `deploy` takes the apk and uploads it to s3, using the provided credentials. It also sets the content type and public access. After a successful upload, GAE is notified of such event, storing the latest version and the correspondent URL.

### How instances are notified of a new apk
1. Information on available apks is stored in the DeviceApplication kind in the backend
2. DeviceApplication has four properties: `deviceType`,  `appCode`, `version`, `fileName`. For a FLOW apk, these properties are:
    * deviceType = androidPhone
    * appCode = fieldSurvey
    * version = x.y.z
    * filename = http://akvoflow.s3.amazonaws.com/apk/INSTANCE_ID/fieldsurvey-xx.yy.zz.apk

3. `deploy` will handle this notification, accessing the datastore remotely with the Remote API tool. This is why the program needs Google username and password parameters, to successfuly login to GAE.
4. This needs to be done for each instance. In this way, the update can be managed per instance.

### Stopping an instance from sending new versions to devices
1. If we want to stop an instance to send new versions to devices, we can set the property `autoUpdateApk` in appengine-web.xml to `false`, and redeploy the instance.

### Proces for a new apk
1. release code
2. run flowreleases.sh
