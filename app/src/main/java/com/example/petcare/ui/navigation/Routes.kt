package com.example.petcare.ui.navigation

object Routes {
    const val Onboarding = "onboarding"
    const val SignIn = "signin"
    const val SignUp = "signup"

    const val Home = "home"
    const val Pets = "pets"
    const val Records = "records"
    const val Calendar = "calendar"
    const val Profile = "profile"

    const val PetProfile = "petProfile/{petId}"
    const val VaccineDetails = "vaccineDetails/{petId}/{vaccineId}"
    const val EventDetails = "eventDetails/{petId}/{eventId}"

    const val NfcScan = "nfc/scan"
    const val NfcWrite = "nfc/write"
}

val bottomBarRoutes = setOf(
    Routes.Home,
    Routes.Pets,
    Routes.Records,
    Routes.Calendar,
    Routes.Profile
)