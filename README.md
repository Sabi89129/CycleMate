# Cycle Mate

An Android app that displays the **OpenCycleMap**.  
The app provides a simple interface to explore cycling routes and paths directly on your device.  

## Features
- Integration of OpenCycleMap
- Optimized for Android devices

<img width="280" alt="image" src="https://github.com/user-attachments/assets/6f24be28-edd6-460b-9c53-1751aca7013c" />


## License
This project uses map data © OpenStreetMap contributors

## Let's start
`git clone https://github.com/Sabi89129/CycleMate.git`

Add a `gradle.properties` and add the following lines:

```# AndroidX activation
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# Open Cycle Maps
# create an API key here: https://www.thunderforest.com/
# It is for free (with limited access)
THUNDERFOREST_KEY=


