package com.tecsup.galvanguerrero.eventplanner_azanero.data

import com.google.firebase.Timestamp

data class Event(
    val id: String = "",
    val title: String = "",
    val date: Timestamp = Timestamp.now(),
    val description: String = "",
    val userId: String = ""
) {
    // Constructor sin argumentos necesario para Firestore
    constructor() : this("", "", Timestamp.now(), "", "")
}