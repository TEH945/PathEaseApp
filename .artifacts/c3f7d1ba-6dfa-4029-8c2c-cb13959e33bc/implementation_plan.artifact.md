# Implement Starred Locations and Route History in HomeScreen

Integrate "Starred" and "Route History" features into the `HomeScreen` to provide a user experience similar to "real maps" apps. This includes showing search result details, allowing users to save (star) locations, and automatically recording route history.

## User Review Required

> [!IMPORTANT]
> The `HomeScreen` will now require both `HomeViewModel` and `ProfileViewModel` to handle navigation state and persistent user data (Starred/History).

> [!NOTE]
> I will add `latitude` and `longitude` fields to the `SupabaseStartedLocation` model to ensure we can re-display starred locations on the map correctly in the future.

## Proposed Changes

### Data Layer

#### [MODIFY] [Models.kt](file:///C:/Android studio/PathEaseApp/app/src/main/java/com/example/patheaseapp/data/remote/Models.kt)
- Update `SupabaseStartedLocation` to include `latitude: Double` and `longitude: Double`.
- Update `RouteHistoryItem` to include `userId: String` if missing (it currently has `id`, `origin`, `destination`, `timestamp`).

### ViewModels

#### [MODIFY] [HomeViewModel.kt](file:///C:/Android studio/PathEaseApp/app/src/main/java/com/example/patheaseapp/ui/home/HomeViewModel.kt)
- Define `SelectedPlace` data class (name, address, latLng).
- Add `selectedPlace` to `HomeUiState`.
- Add `selectPlace(place: SelectedPlace?)` function.

#### [MODIFY] [ViewModel.kt](file:///C:/Android studio/PathEaseApp/app/src/main/java/com/example/patheaseapp/ui/profile/ViewModel.kt) (ProfileViewModel)
- Add `addStarredLocation(userId: String, name: String, address: String, lat: Double, lng: Double)` function using Supabase.
- Add `addRouteHistoryItem(userId: String, origin: String, destination: String)` function.
- Add `fetchRouteHistory(userId: String)` function (currently it's just in memory and empty).

### UI Components

#### [MODIFY] [PathEaseApp.kt](file:///C:/Android studio/PathEaseApp/app/src/main/java/com/example/patheaseapp/PathEaseApp.kt)
- Pass `profileViewModel` to `HomeScreen`.

#### [MODIFY] [homeScreen.kt](file:///C:/Android studio/PathEaseApp/app/src/main/java/com/example/patheaseapp/ui/home/homeScreen.kt)
- Update `HomeScreen` signature.
- Update `MapSearchBar` to return full `Place` details.
- Add a `PlaceDetailCard` (using `ModalBottomSheet` or a floating `Card`) that appears when a place is selected.
- Wire up "Save" button to `profileViewModel.addStarredLocation`.
- Wire up "Start Navigation" button to `homeViewModel.startNavigationTo` and `profileViewModel.addRouteHistoryItem`.

## Verification Plan

### Automated Tests
- N/A (Manual UI verification preferred for map interactions).

### Manual Verification
1.  **Search**: Open the app, search for a location using the Google Maps search bar.
2.  **Selection**: Verify that selecting a result places a marker and shows a card with place details.
3.  **Starring**: Click "Save" on the card, then navigate to the "Starred" tab to verify it appears there.
4.  **History**: Click "Directions/Start", then navigate to the "History" tab to verify the route is recorded.
5.  **Persistence**: Verify that starred locations persist after app restart (via Supabase).
