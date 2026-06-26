package tw.futuremedialab.mycall.data.network

import tw.futuremedialab.mycall.data.network.dto.FraudReportRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FraudApiService {

    @POST("api/fraud/incoming-call")
    suspend fun reportIncomingCall(
        @Header("Authorization") authorization: String,
        @Body request: FraudReportRequestDto
    ): Response<Unit>

    @POST("api/fraud/call-end")
    suspend fun callEnd(
        @Header("Authorization") authorization: String
    ): Response<Unit>
}
