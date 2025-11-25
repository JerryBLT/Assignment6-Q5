# Assignment6-Q5
The app displays the user’s current location on a map, shows their address, and allows placing custom markers by tapping the map.

## Features
- Requests location permission at runtime
- Centers the Google Map on the user’s current location
- Shows a “**You are here**” marker at the user’s GPS position
- Displays the user’s address (using Geocoder.getFromLocation)
- Lets the user add custom markers anywhere by tapping the map
- Works on emulator using the Extended Controls → Location menu

## How It Works
- Uses FusedLocationProviderClient to get the last known location
- Uses Google Maps Compose to render the map and markers
- Uses rememberCameraPositionState() to zoom to the user
- Reverse-geocodes coordinates to a human-readable address
- Custom markers are stored in a mutableStateListOf<LatLng>()

## Permissions Required
- ACCESS_FINE_LOCATION
- ACCESS_COARSE_LOCATION

## API Key
- Add your Google Maps API key inside AndroidManifest.xml:
`<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />`
- (Use a restricted key for safety.)
