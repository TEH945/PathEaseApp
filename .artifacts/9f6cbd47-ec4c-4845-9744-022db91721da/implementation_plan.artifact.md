# Fix App Initialization Blank Screen and Crash

The user reports a blank screen on startup, followed by the login page, and then a crash after 1 second. This plan addresses the lack of a loading state during Supabase initialization and potential ViewModel initialization issues.

## User Review Required

> [!IMPORTANT]
> The Supabase key in `HazardSupabaseClient.kt` starts with `sb_publishable_`, which is not a standard Supabase anon key format. While not necessarily the cause of the immediate crash (as it's used in the Home screen), it will likely cause issues with hazard reporting. Please verify this key.

## Proposed Changes

### Core Navigation & State Management

#### [MODIFY] [PathEaseApp.kt](file:///C:/Mobile_Application_Developement/PathEaseApp/app/src/main/java/com/example/patheaseapp/PathEaseApp.kt)
- Add handling for `SessionStatus.Loading` to show a progress indicator instead of a blank screen or premature login screen.
- Move ViewModel initialization to the top level of the composable for better lifecycle stability.
- Use `remember` for ViewModel factories.
- Ensure `userId` is only accessed when the session is authenticated.

#### [MODIFY] [MainActivity.kt](file:///C:/Mobile_Application_Developement/PathEaseApp/app/src/main/java/com/example/patheaseapp/MainActivity.kt)
- Ensure `supabaseClient` is initialized properly.
- Improve deep link handling to avoid potential race conditions with the UI state.

### Data Layer

#### [MODIFY] [HazardRepository.kt](file:///C:/Mobile_Application_Developement/PathEaseApp/app/src/main/java/com/example/patheaseapp/Hazard/HazardRepository.kt)
- Fix the naming confusion in the constructor where the parameter was ignored in favor of a global variable.

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors.
- Run unit tests for `ProfileViewModel` and `HomeViewModel` (if they exist).

### Manual Verification
1. Launch the app.
2. Verify that a loading indicator is shown during the "blank screen" phase.
3. Verify that the login screen appears correctly if no session exists.
4. Verify that the app does not crash after 1 second.
5. Log in and verify transition to the Home screen.
