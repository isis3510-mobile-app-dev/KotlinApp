package com.example.petcare.data.analytics

import com.example.petcare.data.model.analytics.ButtonDto
import com.example.petcare.data.model.analytics.FeatureDto
import com.example.petcare.data.model.analytics.FeatureRouteDto
import com.example.petcare.data.model.analytics.ScreenDto

/**
 * Hardcoded catalog of all Kotlin-app screens, buttons, features, and routes.
 * Seeded to the backend on first launch so Power BI can reference them.
 *
 * Screen names match [com.example.petcare.ui.navigation.Routes] constants.
 * Button IDs are stable identifiers used by the click tracker.
 */
object AnalyticsSeedData {

    // ── Screens ──────────────────────────────────────────────────────────

    val screens: List<ScreenDto> = listOf(
        // Onboarding
        ScreenDto(name = "onboarding1", buttons = listOf(
            ButtonDto(buttonId = "btn_next_onb1", name = "Next")
        )),
        ScreenDto(name = "onboarding2", buttons = listOf(
            ButtonDto(buttonId = "btn_next_onb2", name = "Next"),
            ButtonDto(buttonId = "btn_back_onb2", name = "Back")
        )),
        ScreenDto(name = "onboarding3", buttons = listOf(
            ButtonDto(buttonId = "btn_get_started", name = "Get Started"),
            ButtonDto(buttonId = "btn_back_onb3", name = "Back")
        )),

        // Auth
        ScreenDto(name = "signin", buttons = listOf(
            ButtonDto(buttonId = "btn_sign_in", name = "Sign In"),
            ButtonDto(buttonId = "btn_go_signup", name = "Go to Sign Up")
        )),
        ScreenDto(name = "signup", buttons = listOf(
            ButtonDto(buttonId = "btn_sign_up", name = "Sign Up"),
            ButtonDto(buttonId = "btn_go_signin", name = "Go to Sign In")
        )),

        // Main tabs
        ScreenDto(name = "home", buttons = listOf(
            ButtonDto(buttonId = "btn_add_pet", name = "Add Pet"),
            ButtonDto(buttonId = "btn_view_pet", name = "View Pet Card"),
            ButtonDto(buttonId = "btn_view_event", name = "View Event"),
            ButtonDto(buttonId = "btn_view_vaccine", name = "View Vaccine Alert")
        )),
        ScreenDto(name = "pets", buttons = listOf(
            ButtonDto(buttonId = "btn_pet_card", name = "Pet Card"),
            ButtonDto(buttonId = "btn_add_pet_fab", name = "Add Pet FAB")
        )),
        ScreenDto(name = "records", buttons = listOf(
            ButtonDto(buttonId = "btn_record_item", name = "Record Item")
        )),
        ScreenDto(name = "calendar", buttons = listOf(
            ButtonDto(buttonId = "btn_calendar_event", name = "Calendar Event")
        )),
        ScreenDto(name = "profile", buttons = listOf(
            ButtonDto(buttonId = "btn_edit_profile", name = "Edit Profile"),
            ButtonDto(buttonId = "btn_theme_toggle", name = "Theme Toggle"),
            ButtonDto(buttonId = "btn_sign_out", name = "Sign Out")
        )),

        // Pet profile & detail screens
        ScreenDto(name = "petProfile", buttons = listOf(
            ButtonDto(buttonId = "btn_edit_pet", name = "Edit Pet"),
            ButtonDto(buttonId = "btn_delete_pet", name = "Delete Pet"),
            ButtonDto(buttonId = "btn_add_vaccine", name = "Add Vaccine"),
            ButtonDto(buttonId = "btn_add_event", name = "Add Event"),
            ButtonDto(buttonId = "btn_open_weight_tracker", name = "Open Weight Tracker"),
            ButtonDto(buttonId = "btn_view_vaccine_detail", name = "View Vaccine Detail"),
            ButtonDto(buttonId = "btn_view_event_detail", name = "View Event Detail"),
            ButtonDto(buttonId = "btn_nfc_write", name = "Write NFC"),
            ButtonDto(buttonId = "btn_nfc_scan", name = "Scan NFC")
        )),
        ScreenDto(name = "vaccineDetails", buttons = listOf(
            ButtonDto(buttonId = "btn_edit_vaccine", name = "Edit Vaccine"),
            ButtonDto(buttonId = "btn_delete_vaccine", name = "Delete Vaccine"),
            ButtonDto(buttonId = "btn_back_vaccine", name = "Back")
        )),
        ScreenDto(name = "eventDetails", buttons = listOf(
            ButtonDto(buttonId = "btn_edit_event", name = "Edit Event"),
            ButtonDto(buttonId = "btn_delete_event", name = "Delete Event"),
            ButtonDto(buttonId = "btn_back_event", name = "Back")
        )),
        ScreenDto(name = "weightTracker", buttons = listOf(
            ButtonDto(buttonId = "btn_submit_weight_log", name = "Submit Weight Log"),
            ButtonDto(buttonId = "btn_edit_weight_log", name = "Edit Weight Log"),
            ButtonDto(buttonId = "btn_delete_weight_log", name = "Delete Weight Log")
        )),

        // NFC
        ScreenDto(name = "nfc/scan", buttons = listOf(
            ButtonDto(buttonId = "btn_start_scan", name = "Start Scan")
        )),
        ScreenDto(name = "nfc/scanning", buttons = emptyList()),
        ScreenDto(name = "nfc/scanSuccess", buttons = listOf(
            ButtonDto(buttonId = "btn_scan_done", name = "Done")
        )),
        ScreenDto(name = "nfc/write", buttons = listOf(
            ButtonDto(buttonId = "btn_start_write", name = "Start Write")
        )),
        ScreenDto(name = "nfc/writing", buttons = emptyList()),
        ScreenDto(name = "nfc/writeSuccess", buttons = listOf(
            ButtonDto(buttonId = "btn_write_done", name = "Done")
        )),

        // Add Pet flow
        ScreenDto(name = "addPet1", buttons = listOf(
            ButtonDto(buttonId = "btn_next_pet1", name = "Continue"),
            ButtonDto(buttonId = "btn_back_pet1", name = "Back")
        )),
        ScreenDto(name = "addPet2", buttons = listOf(
            ButtonDto(buttonId = "btn_next_pet2", name = "Continue"),
            ButtonDto(buttonId = "btn_back_pet2", name = "Back")
        )),
        ScreenDto(name = "addPet3", buttons = listOf(
            ButtonDto(buttonId = "btn_submit_pet", name = "Submit"),
            ButtonDto(buttonId = "btn_back_pet3", name = "Back")
        )),

        // Add Vaccine flow
        ScreenDto(name = "addVaccine1", buttons = listOf(
            ButtonDto(buttonId = "btn_next_vax1", name = "Continue"),
            ButtonDto(buttonId = "btn_back_vax1", name = "Back")
        )),
        ScreenDto(name = "addVaccine2", buttons = listOf(
            ButtonDto(buttonId = "btn_next_vax2", name = "Continue"),
            ButtonDto(buttonId = "btn_back_vax2", name = "Back")
        )),
        ScreenDto(name = "addVaccine3", buttons = listOf(
            ButtonDto(buttonId = "btn_submit_vaccine", name = "Submit"),
            ButtonDto(buttonId = "btn_back_vax3", name = "Back")
        )),

        // Add Event flow
        ScreenDto(name = "addEvent1", buttons = listOf(
            ButtonDto(buttonId = "btn_next_evt1", name = "Continue"),
            ButtonDto(buttonId = "btn_back_evt1", name = "Back")
        )),
        ScreenDto(name = "addEvent2", buttons = listOf(
            ButtonDto(buttonId = "btn_next_evt2", name = "Continue"),
            ButtonDto(buttonId = "btn_back_evt2", name = "Back")
        )),
        ScreenDto(name = "addEvent3", buttons = listOf(
            ButtonDto(buttonId = "btn_submit_event", name = "Submit"),
            ButtonDto(buttonId = "btn_back_evt3", name = "Back")
        )),

        // Suggestions
        ScreenDto(name = "suggestions", buttons = listOf(
            ButtonDto(buttonId = "btn_suggestion_item", name = "Suggestion Item")
        )),
        ScreenDto(name = "suggestions/{petId}/{petName}", buttons = listOf(
            ButtonDto(buttonId = "btn_back_suggestion", name = "Back")
        ))
    )

    // ── Features ─────────────────────────────────────────────────────────
    // Features represent user-initiated actions backed by an API call.
    // originButton+originScreen identify where the user started.
    // NOTE: originScreen will be replaced with the actual MongoDB _id after seeding.

    val features: List<FeatureDto> = listOf(
        FeatureDto(name = "Create Pet",       originButton = "btn_submit_pet",     originScreen = "addPet3"),
        FeatureDto(name = "Edit Pet",         originButton = "btn_edit_pet",       originScreen = "petProfile"),
        FeatureDto(name = "Delete Pet",       originButton = "btn_delete_pet",     originScreen = "petProfile"),
        FeatureDto(name = "Add Vaccination",  originButton = "btn_submit_vaccine", originScreen = "addVaccine3"),
        FeatureDto(name = "Edit Vaccination", originButton = "btn_edit_vaccine",   originScreen = "vaccineDetails"),
        FeatureDto(name = "Delete Vaccination", originButton = "btn_delete_vaccine", originScreen = "vaccineDetails"),
        FeatureDto(name = "Create Event",     originButton = "btn_submit_event",   originScreen = "addEvent3"),
        FeatureDto(name = "Edit Event",       originButton = "btn_edit_event",     originScreen = "eventDetails"),
        FeatureDto(name = "Delete Event",     originButton = "btn_delete_event",   originScreen = "eventDetails"),
        FeatureDto(name = "Create Weight Log", originButton = "btn_submit_weight_log", originScreen = "weightTracker"),
        FeatureDto(name = "Edit Weight Log", originButton = "btn_edit_weight_log", originScreen = "weightTracker"),
        FeatureDto(name = "Delete Weight Log", originButton = "btn_delete_weight_log", originScreen = "weightTracker"),
        FeatureDto(name = "NFC Write",        originButton = "btn_start_write",    originScreen = "nfc/write"),
        FeatureDto(name = "NFC Scan",         originButton = "btn_start_scan",     originScreen = "nfc/scan"),
        FeatureDto(name = "Sign In",          originButton = "btn_sign_in",        originScreen = "signin"),
        FeatureDto(name = "Sign Up",          originButton = "btn_sign_up",        originScreen = "signup"),
        FeatureDto(name = "Load Home Data",          originButton = "system",             originScreen = "home"),
        FeatureDto(name = "Load Home Data (Cached)", originButton = "system",             originScreen = "home"),
        FeatureDto(name = "Upload Vaccine Attachment", originButton = "btn_submit_vaccine", originScreen = "addVaccine3"),
        FeatureDto(name = "Upload Event Attachment",   originButton = "btn_submit_event",   originScreen = "addEvent3"),
        FeatureDto(name = "Load My Pets",     originButton = "system",            originScreen = "pets"),
        FeatureDto(name = "Load Pet Profile", originButton = "btn_view_pet",       originScreen = "home"),
        FeatureDto(name = "Load User Profile", originButton = "system",           originScreen = "profile"),
        FeatureDto(name = "Update User Profile", originButton = "btn_edit_profile", originScreen = "profile"),
        FeatureDto(name = "Delete Account",   originButton = "btn_sign_out",       originScreen = "profile"),
        FeatureDto(name = "Load Event Details", originButton = "btn_view_event",   originScreen = "home"),
        FeatureDto(name = "Load Vaccination Details", originButton = "btn_view_vaccine", originScreen = "home"),
    )

    // ── Feature Routes ───────────────────────────────────────────────────
    // Routes track the user journey from an origin button to a final action.
    // Click tracking counts taps along this path.

    val featureRoutes: List<FeatureRouteDto> = listOf(
        FeatureRouteDto(
            name = "Add Pet Flow",
            originButton = "btn_add_pet", originScreen = "home",
            endButton = "btn_submit_pet", endScreen = "addPet3"
        ),
        FeatureRouteDto(
            name = "Add Pet Flow (FAB)",
            originButton = "btn_add_pet_fab", originScreen = "pets",
            endButton = "btn_submit_pet", endScreen = "addPet3"
        ),
        FeatureRouteDto(
            name = "Add Vaccine Flow",
            originButton = "btn_add_vaccine", originScreen = "petProfile",
            endButton = "btn_submit_vaccine", endScreen = "addVaccine3"
        ),
        FeatureRouteDto(
            name = "Add Event Flow",
            originButton = "btn_add_event", originScreen = "petProfile",
            endButton = "btn_submit_event", endScreen = "addEvent3"
        ),
        FeatureRouteDto(
            name = "Weight Log Flow",
            originButton = "btn_open_weight_tracker", originScreen = "petProfile",
            endButton = "btn_submit_weight_log", endScreen = "weightTracker"
        ),
        FeatureRouteDto(
            name = "NFC Write Flow",
            originButton = "btn_nfc_write", originScreen = "petProfile",
            endButton = "btn_write_done", endScreen = "nfc/writeSuccess"
        ),
        FeatureRouteDto(
            name = "NFC Scan Flow",
            originButton = "btn_nfc_scan", originScreen = "petProfile",
            endButton = "btn_scan_done", endScreen = "nfc/scanSuccess"
        ),
        FeatureRouteDto(
            name = "Onboarding Flow",
            originButton = "btn_next_onb1", originScreen = "onboarding1",
            endButton = "btn_get_started", endScreen = "onboarding3"
        ),
        FeatureRouteDto(
            name = "Edit Pet Flow",
            originButton = "btn_edit_pet", originScreen = "petProfile",
            endButton = "btn_submit_pet_edit", endScreen = "petProfile"
        ),
        FeatureRouteDto(
            name = "Delete Pet Flow",
            originButton = "btn_delete_pet", originScreen = "petProfile",
            endButton = "btn_confirm_delete_pet", endScreen = "pets"
        ),
        FeatureRouteDto(
            name = "Edit Vaccination Flow",
            originButton = "btn_edit_vaccine", originScreen = "vaccineDetails",
            endButton = "btn_save_vaccine_edit", endScreen = "vaccineDetails"
        ),
        FeatureRouteDto(
            name = "Delete Vaccination Flow",
            originButton = "btn_delete_vaccine", originScreen = "vaccineDetails",
            endButton = "btn_confirm_delete_vax", endScreen = "petProfile"
        ),
        FeatureRouteDto(
            name = "Edit Event Flow",
            originButton = "btn_edit_event", originScreen = "eventDetails",
            endButton = "btn_save_event_edit", endScreen = "eventDetails"
        ),
        FeatureRouteDto(
            name = "Delete Event Flow",
            originButton = "btn_delete_event", originScreen = "eventDetails",
            endButton = "btn_confirm_delete_evt", endScreen = "petProfile"
        ),
    )
}
