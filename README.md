# WayBack 📍
**Never lose your way again.**

WayBack is a precise location-tracking and recovery application for Android. Designed to eliminate the stress of finding your parked car or a hidden trailhead, WayBack combines real-time GPS tracking, local persistence, and automated proximity alerts.

---

## 🌟 Key Features
* **Real-Time GPS Pinning**: Capture your exact coordinates ($Latitude$/$Longitude$) with a single tap using the `FusedLocationProviderClient`.
* **Persistent Location History**: Save and name your spots (e.g., "Level 4, Slot B2") in a local **Room Database**.
* **Automated Geofencing**: High-precision background monitoring triggers a notification via `GeofencingClient` when you return within a **200-meter** radius of a saved spot.
* **One-Tap Navigation**: Seamlessly launch turn-by-turn directions from your current location to any saved point using **Google Maps Integration**.
* **Material Design UI**: An intuitive dual-view interface featuring a Bottom Navigation bar to toggle between active tracking and location history.

---

## 🛠️ Technical Stack
* **Language**: Java
* **Minimum SDK**: Android 8.0 (API Level 26)
* **Architecture**: MVVM (Model-View-ViewModel)
* **Database**: Room Persistence Library
* **Location Services**: Google Play Services (Location & Geofencing)
* **UI Components**: RecyclerView, FloatingActionButton, BottomNavigationView, ConstraintLayout

---

## 🏗️ Architecture & Design
The project follows the **MVVM Pattern** to ensure a clean separation of concerns:
1.  **Model**: `LocationEntity` and `AppDatabase` manage the SQLite data.
2.  **View**: `MainActivity` handles UI interactions and permission requests.
3.  **ViewModel**: `LocationViewModel` provides a lifecycle-aware interface between the UI and the data.
4.  **Repository**: `LocationRepository` abstracts the data sources and handles background execution for database operations.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug or newer.
* A physical Android device or Emulator with Google Play Services.
* (Optional) A Google Maps API Key for in-app map rendering.

### Installation
1.  Clone the repository:
    ```bash
    git clone [https://github.com/TadiwaMakoni05/JavaAssignment.git]
    ```
2.  Open the project in **Android Studio**.
3.  Ensure the following permissions are granted on the device:
    * `ACCESS_FINE_LOCATION`
    * `ACCESS_BACKGROUND_LOCATION` (Required for Geofencing notifications)
    * `POST_NOTIFICATIONS` (For Android 13+)

---

## 📝 Usage
1.  **Save a Spot**: On the Home tab, tap the **+** button. Name your location and hit Save.
2.  **Navigate**: Tap any item in the **History** list to open Google Maps for turn-by-turn directions.
3.  **Manage**: Long-press any item in the list to **Edit** the name or **Delete** the record.
4.  **Geofence**: Walk away from your spot. When you return within 200m, look for the "WayBack Alert" in your notification tray.

---

## 🎓 Academic Requirements Met
* [x] **GPS Tracking**: FusedLocationProviderClient integration.
* [x] **Geofencing**: Automated circular region monitoring.
* [x] **Local Storage**: Room/SQLite persistence.
* [x] **Notifications**: O-level Notification Channels.
* [x] **Navigation**: External Google Maps Intent API.

---

## 👨‍💻 Author
**Tadiwanashe Makoni,Kudzai Nyama & Fortunate Misihairahwi** *Java Programming Assignment - Mobile Application Development*
