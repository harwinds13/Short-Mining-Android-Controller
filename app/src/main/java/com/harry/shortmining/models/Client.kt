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
    val pod: String = "",
    val job: String = "",
    val sch: String = "",
    val applicationId:String = "",
    val fullLocal: LocalStorage = LocalStorage()
) {
    constructor() : this("", 0, "", "", "","")
}

class LocalStorage (
    val accessToken:String = "",
    val awswaf_session_storage:String = "",
    val bbCandidateId:String = "" ,
    val awswaf_token_refresh_timestamp:String = "",
    val idToken:String= "",
    val refreshToken:String="",
    val sessionToken:String="",
    val sfCandidateId:String=""
    ){

}
