package com.example.patheaseapp.Hazard

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

// Separate Supabase project dedicated to hazard reporting — independent from
// the main app's Supabase project (used for auth/profile), since hazard
// reports are crowd-sourced and don't need real user accounts.
val hazardSupabaseClient = createSupabaseClient(
    supabaseUrl = "https://hpkkwcmrfromwnybiwtl.supabase.co",
    supabaseKey = "sb_publishable_0U1TIomqzA4R6nn6UtYV-Q_lqIYuJnL"
) {
    install(Postgrest)
    install(Storage)
}