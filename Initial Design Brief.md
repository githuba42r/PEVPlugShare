You are an android developer and have been takses with creating a Android App to assit creation of OSMAnd "My Places" related to PEV Charging locations.

The App is targeted at Android 16 and will run primarily on mobile phones.

The user currently used OSMAnd for routing and navigation while riding PEV. They have a categoty of "My Places" in their OSMAnd account to store the locations of charging points at various locations.

The user wants a quick and easy method to add a new charging location to their account.

The app shoudl first ask the user to authenticate to their existing OSMAnd online account via oAuth2 - using authorization grant flow.

The main screen will show a list of location from the "My Places" Category that they have selected stores the PEV charging locations, if the user has not selected a "My Places" Category, then they will be promopted with a list of the current categories from their account and asked to select the PEV Charging Caategory.

the list will show the list of locations ordered by distance for the current location. each location on the list will have an option to set as a navigation target, and if selected will request the OSMAnd on the device start a navigation to the selected location.

the list should include the name, distance and capacity (number of GPO) at the location. the list should highlight locations that are open at the current time, while still allowing navigation to closed locations. should the user want. a filter section at the top of the list should allow the user to add filters to the list of locations, being open/all open hours, capacity minimum.

ad the bottom of the screen will be an add button to allow the addition of a new charging location.

the add process will capture the current location from the GPS, a name of the location, a desctiption and options for opening hours (e.g. 9-5, 24x7 wedays only, etc)

it will prompt for the number of GPO (Powerpoints - socket:as3112  points).

the add form will allow the user to add photos of the location to the record.

the app should attach the default color and icon style from the "My Places" category chosen against the new record.

to avoid duplicated the app will confirm that there are no other locations in the My Places Category within 250m of the current location, if additional locations are found a confirmation message indicated that a duplicate was found will be displayed, but if confirmed the new location will be added.

the app should allow for the editing of existing locations in the My Places, including adding additional phones.

the app allow for the deletion of a location following confirmation.

the app will have a topbar with a hamburger menu.

in the menu are a settings, about and logout, and resync options.

the settings will allow the user to select the My Places Category to update the list. and the automated resync frequency (default 1 day), with options of 4/8/12 hours and 1/2/5/14/28 days between resync operations

the logout will log out of the OSMAnd account.

the resync option will schedule a background resync operation to download the locations from the connected OSMAnd account.

the app should periodically attempt a resync the My Places data with teh OSMAnd account to download any updates, includeing deletions.

when a resync operation is running a silent android notification will be displaed in the notification drawer indicating that at resync is in progress.

the about will section will contain the
  - Version Information
    - version number
    - build date and time, and basic information about the usage of the app.
  - Account Inforamtion
    - OSMAnd Account connected
    - last sync time
  - App description and features list

the primary goal is to allow a person using the app to quickly add a new location. entry of text and long descriptions (eg. for opening hours etc) should be avoided and options that are easily selected provided.

create a plan to design and implement the application for review, in the review stage ask any questions to clarify any additional required information or make suggestions to improve the apps user experience.


questions responses
OSMAnd api documentation is available at: https://osmand.net/docs/technical/osmand-api-sdk/
Photo storage in this version will be local only, with upload to osmand if possible - future version will use either a private photo storage service or online service like mapillary, dropbox onedrive, google drive etc.
 - photos should include GPX data for the location and additional description of the App name and location name and description and other information in the photo metadata if possible.
Offline support - an indicator in the topbar and the about screen about currently offline would be a good idea, storage of new locations while office should be allowed with automatic upload when device comes back online.
Allow support for AS3112 (AU GPO) as the primary socket type, but allow addition of Type2 EV and other EV charger types.
  - add an flag on each location if the location is public or private (public meaning access is in a public area like park, shared charging center etc. or private maening owned and operated by a private business)
  - add a flag to indicate whether there is a fee to use the charging location
  - provide an additional data section to collect additional information
     - for a paid charging provider the priovider name and website link.
     - approximate charging costs
     - other potentially relevant information.
Loation Permissions.
 - Fine Location resolution when adding a location.
 - Course Location resoluition on the main list used to calculat the distances to each location.
 - distance calculation should be line of sight calculations and not a navigation distance.

 UX Improvements

 Quick add button is a good suggestion, but the normal add button should function like a quick add button, with only the Location Name and the Number of GPO being required entries - photos, description, opening hours and additional info are all optional on the initial add screen - the user may wish to edit and update the information once they have more time (ride break or ride completion)
 Favourites are a good idea and UI shoudl allow easy marking a location as a favourite and allow filtering of the list for gavourites.
 Search function would be a good additionl, but it is unlikely to be used, and should probably be accessible form the menu and not the main screen.
 Map view would be desired, but I do not want to have to use paid API and service in the app and would like that all functions as much as practicable be local to the device.
 Batch operations not need in this version - not expected to be a furture feature enhancement
 Export option for sharing would be great. Additional a share locations that can share a JSON encoded file of locations, and intent setup that when the shared locations are opened that it open the app and import the locations with appropriate merge/replace options for each location.



