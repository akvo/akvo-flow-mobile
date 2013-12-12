## Users

#### High-level requirements
1. different roles for enumerators and field manager (admin)
2. Field Manager users are created on the dashboard, Enumerators can be created on the device and on the dashboard
3. login services for users
4. when the app is first downloaded, it can be registered with an instance by entering a code.


#### Requirements
###### 1. different roles for enumerators and field manager (admin)
* Two different login roles should be available for device users: Enumerator and Field Manager (Admin)
* The Enumerator should have only access to the functionality needed to find a survey, open a survey, fill a survey and submit a survey
* The Field Manager should additionaly have acces to: create new device users, manually download surveys, delete surveys, etc.

###### 2. Admin users are created on the dashboard, Enumerators can be created on the device and on the dashboard
* On the dashboard, an Admin user can create Field Manager users. 
* On the dashboard, a Field Manager can be assigned access to certain survey groups. 
* On the dashboard, an Admin user can create Enumerator users
* On the dashboard, an Enumerator can be assigned access to certain survey groups. 
* On the device, a Field Manager user can create Enumerator users
* On the device, a Field Manager user can assing Enumerator users to the survey groups that the Field Manager has access to

###### 3. login services for users
* For each user, an email addess is recorded
* If a user forgets his/her email, they can request a new password to be send to their email
* A Field Manager can reset the password of an Enumerator

###### 4. when the app is first downloaded, it can be registered with an instance by entering a code
* When the app is first downloaded, it is not connected to a dashboard
* The app displays a message and an entry field for a key
* The key consists of a base-32 key of length 8, in which l, o, and z are replaced by 1, 0 and 2.
* Using the key, the device connects to a service which returns the connection details.
* When the app starts again after it has successfully registered, it shows the user login screen.
* In the settings for the Admin, there is a setting 'register with additional dashboard'. When this is selected, an additional key can be selected.
* If multiple dashboards have been registered, the user login screen shows an additional dropdown, from which the dashboard can be chosen. 


