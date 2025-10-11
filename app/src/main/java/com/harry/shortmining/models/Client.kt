package com.harry.shortmining.models

data class Client(
    var bbCandidateId: String = "",
    val expireTime: Long = 0,
    val jobType: String = "",
    val location: String = "",
    val status: String = "",
    val clientName: String = "",
    val clientPhoneNumber: String = "",
    val clientEmail: String = "",
    val error: String = "",
    val pod: String = ""
) {
    constructor() : this("", 0, "", "", "","")
}