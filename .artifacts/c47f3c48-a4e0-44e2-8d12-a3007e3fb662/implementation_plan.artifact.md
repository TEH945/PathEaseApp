# Fix Reset Password Flow and Rate Limit Issues

This plan addresses two problems:
1. **Email Rate Limit Exceeded**: The error `over_email_send_rate_limit` shown when requesting a reset link.
2. **Invalid Path Error**: The error `{"error": "requested path is invalid"}` shown after clicking the reset link in the email.

## User Review Required

> [!IMPORTANT]
> To fix these issues, you **MUST** perform actions in your **Supabase Dashboard**. Code changes alone are not enough.

## Proposed Changes

### 1. Authentication Configuration (Supabase Dashboard)

You need to adjust your Supabase settings to allow more frequent emails and recognize the redirect URL.

*   **Increase Rate Limit**:
    1. Go to your [Supabase Dashboard](https://supabase.com/dashboard).
    2. Navigate to **Project Settings** > **Auth**.
    3. Scroll down to **Rate Limits**.
    4. Increase the **Emails per hour** (e.g., from 3 to 20) to avoid the `over_email_send_rate_limit` error during testing.
*   **Add Redirect URL**:
    1. In the same **Auth** settings page, look for **Redirect URLs**.
    2. Add `patheaseapp://reset-password` to the **Additional Redirect URLs** list. This allows the app to catch the link.

---

### 2. Android App Configuration

We will update the app to use a custom deep link scheme (`patheaseapp://reset-password`) which is more reliable for mobile development than the Supabase project URL.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Android%20studio/PathEaseApp/app/src/main/AndroidManifest.xml)
Fix the invalid `intent-filter` and add a custom scheme.

#### [MODIFY] [ForgotPasswordScreen.kt](file:///C:/Android%20studio/PathEaseApp/app/src/main/java/com/example/patheaseapp/ui/auth/ForgotPasswordScreen.kt)
Update the `redirectUrl` to match the custom scheme.

## Verification Plan

### Manual Verification
1. **Request Reset Link**: Open the app, go to Reset Password, enter email, and click "Send Reset Link". Verify no rate limit error appears (after increasing it in Supabase).
2. **Open Email**: Open the reset email on your device and click "Reset Password".
3. **App Redirection**: Verify the link opens the app and navigates to the "Set New Password" screen (Diagram 2 should no longer appear).
4. **Update Password**: Enter a new password and verify success.
