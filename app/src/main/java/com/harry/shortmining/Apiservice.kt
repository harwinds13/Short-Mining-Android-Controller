package com.harry.shortmining

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture

class ApiService (private val authToken: String){
    val client = OkHttpClient()
    val mediaType = "application/json".toMediaType()
    fun invokeLocationApi(zipCode: String = "l6w", onResult: (String?) -> Unit) {
        val body = "{\"query\":\"query queryGeoInfoByAddress(\$geoAddressQueryRequest: GeoAddressQueryRequest!) {\\n  queryGeoInfoByAddress(geoAddressQueryRequest: \$geoAddressQueryRequest) {\\n    country\\n    lat\\n    lng\\n    postalCode\\n    label\\n    municipality\\n    region\\n    subRegion\\n    addressNumber\\n    __typename\\n  }\\n}\\n\",\"variables\":{\"geoAddressQueryRequest\":{\"address\":\"$zipCode\",\"countries\":[\"CAN\"]}}}"
        Log.i("HZ_EXT", "Request body: $body")

        val request = Request.Builder()
            .url("https://e5mquma77feepi2bdn4d6h3mpu.appsync-api.us-east-1.amazonaws.com/graphql")
            .post(body.toRequestBody(mediaType))
            .addHeader("accept", "*/*")
            .addHeader("accept-language", "en-US,en;q=0.9")
            .addHeader("authorization", authToken)
            .addHeader("cache-control", "no-cache")
            .addHeader("content-type", "application/json")
            .addHeader("country", "Canada")
            .addHeader("iscanary", "false")
            .addHeader("origin", "https://hiring.amazon.ca")
            .addHeader("pragma", "no-cache")
            .addHeader("priority", "u=1, i")
            .addHeader("referer", "https://hiring.amazon.ca/")
            .addHeader(
                "sec-ch-ua",
                "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\""
            )
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("sec-ch-ua-platform", "\"Windows\"")
            .addHeader("sec-fetch-dest", "empty")
            .addHeader("sec-fetch-mode", "cors")
            .addHeader("sec-fetch-site", "cross-site")
            .addHeader(
                "user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
            )
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("HZ_EXT", "Request failed: ${e.message}")
                onResult(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("ATOZ_EXT", "Unexpected code: ${it.code}")
                        onResult(null)
                        return
                    }
                    Log.i("HZ_EXT", "Response code: ${it.code}")
                    onResult(it.body?.string())
                }
            }
        })

    }

    fun fetchLocation(zipCode: String):Triple<Double, Double, String> {
        val future = CompletableFuture<Triple<Double, Double, String>?>()
        try {
            invokeLocationApi(zipCode) { responseBody ->
                if (responseBody == null) {
                    Log.e("HZ_EXT", "Response body is null")
                    return@invokeLocationApi
                }
                val jsonObject = JSONObject(responseBody)
                val data = jsonObject.getJSONObject("data")
                val queryGeoInfoByAddress = data.getJSONArray("queryGeoInfoByAddress")
                if (queryGeoInfoByAddress.length() > 0) {
                    val firstResult = queryGeoInfoByAddress.getJSONObject(0)
                    val lat = firstResult.getDouble("lat")
                    val lng = firstResult.getDouble("lng")
                    val label = firstResult.getString("label")
                    future.complete(Triple(lat, lng, label))
                }
            }

        } catch (e: Exception) {
            Log.e("HZ_EXT", "Error parsing response: ${e.message}")
            future.complete(null)
        }
        return future.get() ?: Triple(0.0, 0.0, "Unknown Location")
    }

    fun invokeGraphQlTOGetShifts(log:Double,  lat:Double, distance:String): String {
        val future = CompletableFuture<String?>()
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = "{\"query\":\"query searchJobCardsByLocation(\$searchJobRequest: SearchJobRequest!) {\\n  searchJobCardsByLocation(searchJobRequest: \$searchJobRequest) {\\n    nextToken\\n    jobCards {\\n      jobId\\n      language\\n      dataSource\\n      requisitionType\\n      jobTitle\\n      jobType\\n      employmentType\\n      city\\n      state\\n      postalCode\\n      locationName\\n      totalPayRateMin\\n      totalPayRateMax\\n      tagLine\\n      bannerText\\n      image\\n      jobPreviewVideo\\n      distance\\n      featuredJob\\n      bonusJob\\n      bonusPay\\n      scheduleCount\\n      currencyCode\\n      geoClusterDescription\\n      surgePay\\n      jobTypeL10N\\n      employmentTypeL10N\\n      bonusPayL10N\\n      surgePayL10N\\n      totalPayRateMinL10N\\n      totalPayRateMaxL10N\\n      distanceL10N\\n      monthlyBasePayMin\\n      monthlyBasePayMinL10N\\n      monthlyBasePayMax\\n      monthlyBasePayMaxL10N\\n      jobContainerJobMetaL1\\n      virtualLocation\\n      poolingEnabled\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\",\"variables\":{\"searchJobRequest\":{\"locale\":\"en-CA\",\"country\":\"Canada\",\"keyWords\":\"\",\"equalFilters\":[],\"containFilters\":[{\"key\":\"isPrivateSchedule\",\"val\":[\"false\"]}],\"rangeFilters\":[],\"orFilters\":[],\"dateFilters\":[{\"key\":\"firstDayOnSite\",\"range\":{\"startDate\":\"2025-08-20\"}}],\"sorters\":[],\"pageSize\":100}}}".toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://e5mquma77feepi2bdn4d6h3mpu.appsync-api.us-east-1.amazonaws.com/graphql")
            .post(body)
            .addHeader("accept", "*/*")
            .addHeader("accept-language", "en-US,en;q=0.9")
            .addHeader("authorization", "Bearer Status|unauthenticated|Session|eyJhbGciOiJLTVMiLCJ0eXAiOiJKV1QifQ.eyJpYXQiOjE3NDc2MDUwMzQsImV4cCI6MTc0NzYwODYzNH0.AQICAHidzPmCkg52ERUUfDIMwcDZBDzd+C71CJf6w0t6dq2uqwER06Ekeph9MGK02fweIMm+AAAAtDCBsQYJKoZIhvcNAQcGoIGjMIGgAgEAMIGaBgkqhkiG9w0BBwEwHgYJYIZIAWUDBAEuMBEEDHcyDtXQU/KLd27RigIBEIBt5RDNpdBrLOywA4lPCfHVwwRtpEAIEzj3tl7LnoWOZHA1O10ML36ShPrXSu1l6Dq2Rx4QdHAZZ9f3+GaJRmbGx9f1PkQR5kqSwRVFVNXqbMZlBc0LIoy5Y+oCLEj3TqYuACBT8v+fAK8XIR5++w==")
            .addHeader("cache-control", "no-cache")
            .addHeader("content-type", "application/json")
            .addHeader("country", "Canada")
            .addHeader("iscanary", "false")
            .addHeader("origin", "https://hiring.amazon.ca")
            .addHeader("pragma", "no-cache")
            .addHeader("priority", "u=1, i")
            .addHeader("referer", "https://hiring.amazon.ca/")
            .addHeader("sec-ch-ua", "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"")
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("sec-ch-ua-platform", "\"Windows\"")
            .addHeader("sec-fetch-dest", "empty")
            .addHeader("sec-fetch-mode", "cors")
            .addHeader("sec-fetch-site", "cross-site")
            .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("HZ_EXT", "Unexpected code: ${response.code}")
                return "No_Data_Found"
            }
            val responseBody = response.body?.string()
            return responseBody ?: "No_Data_Found"
        } catch (e: IOException) {
            Log.e("HZ_EXT", "Request failed.")
            return "No_Data_Found"
        }
    }

    fun queryCandidate(bbCandidateI: String): Triple<String, String, String>? {
        val url = "https://zuzm2l7jovcizd7movvfj7qt3y.appsync-api.us-east-1.amazonaws.com/graphql"

        // GraphQL query and variables
        val query = "query queryCandidate(\$bbCandidateId: String!) {\n  queryCandidate(bbCandidateId: \$bbCandidateId) {\n    candidateId\n    candidateSFId\n    firstName\n    middleName\n    lastName\n    preferredFirstName\n    preferredMiddleName\n    preferredLastName\n    nameSuffix\n    emailId\n    phoneNumber\n    phoneCountryCode\n    locale\n    additionalBackgroundInfo {\n      address {\n        addressLine1\n        addressLine2\n        city\n        state\n        country\n        zipcode\n        countryCode\n        __typename\n      }\n      dateOfBirth\n      __typename\n    }\n    timezone\n    language\n    englishName {\n      firstName\n      lastName\n      fromDate\n      toDate\n      __typename\n    }\n    __typename\n  }}"

        val variables = JSONObject().apply {
            put("bbCandidateId", bbCandidateI)
        }

        val requestBody = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }.toString()

        Log.i("HZ_EXT", "Request Body: $requestBody")

//        return
        // Build the request
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody(mediaType))
            .addHeader("accept", "*/*")
            .addHeader("accept-language", "en-US,en;q=0.9")
            .addHeader("accesstoken", authToken)
            .addHeader("authorization", "Bearer token")
            .addHeader("cache-control", "no-cache")
            .addHeader("content-type", "application/json")
            .addHeader("country", "Canada")
            .addHeader("iscanary", "false")
            .addHeader("origin", "https://hiring.amazon.ca")
            .addHeader("pragma", "no-cache")
            .addHeader("priority", "u=1, i")
            .addHeader("referer", "https://hiring.amazon.ca/")
            .addHeader("sec-ch-ua", "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Google Chrome\";v=\"140\"")
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("sec-ch-ua-platform", "\"Windows\"")
            .addHeader("sec-fetch-dest", "empty")
            .addHeader("sec-fetch-mode", "cors")
            .addHeader("sec-fetch-site", "cross-site")
            .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
            .build()

        // Execute the request
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("HZ_EXT", "Unexpected code: ${response.code}")
                return null
            }

            val responseBody = response.body?.string()
            return fetchCandidateDetails(responseBody ?: "")
            Log.i("HZ_EXT", "Response Body: $responseBody")
        }
    }

    fun fetchCandidateDetails(response: String): Triple<String, String, String>? {
        return try {
            val jsonObject = JSONObject(response)
            val queryCandidate = jsonObject.getJSONObject("data").getJSONObject("queryCandidate")
            val firstName = queryCandidate.getString("firstName")
            val phoneNumber = queryCandidate.getString("phoneNumber")
            val emailId = queryCandidate.getString("emailId")
            Triple(firstName, phoneNumber, emailId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}






