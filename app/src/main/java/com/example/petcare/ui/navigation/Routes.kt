package com.example.petcare.ui.navigation

object Routes {
    const val Onboarding1 = "onboarding1"
    const val Onboarding2 = "onboarding2"
    const val Onboarding3 = "onboarding3"

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
    const val WeightTracker = "weightTracker/{petId}"

    const val NfcScan = "nfc/scan"
    const val NfcScanning = "nfc/scanning"
    const val NfcScanSuccess = "nfc/scanSuccess"

    const val NfcWrite = "nfc/write"
    const val NfcWriting = "nfc/writing"
    const val NfcWriteSuccess = "nfc/writeSuccess"


    //Add pet forms
    const val AddPet1 = "addPet1"
    const val AddPet2 = "addPet2"
    const val AddPet3 = "addPet3"

    //Add vaccine forms
    const val AddVaccine1 = "addVaccine1"
    const val AddVaccine2 = "addVaccine2"
    const val AddVaccine3 = "addVaccine3"

    //Add events forms
    const val AddEvent1 = "addEvent1"
    const val AddEvent2 = "addEvent2"
    const val AddEvent3 = "addEvent3"

    //Notifications
    const val Suggestions = "suggestions"
    const val SuggestionPerPet = "suggestions/{petId}/{petName}"
}

val bottomBarRoutes = setOf(
    Routes.Home,
    Routes.Pets,
    Routes.Records,
    Routes.Calendar,
    Routes.Profile
)
