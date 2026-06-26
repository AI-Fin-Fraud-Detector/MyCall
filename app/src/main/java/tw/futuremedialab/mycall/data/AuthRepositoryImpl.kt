package tw.futuremedialab.mycall.data

import com.google.gson.Gson
import tw.futuremedialab.mycall.data.network.AuthApiService
import tw.futuremedialab.mycall.data.network.FraudApiService
import tw.futuremedialab.mycall.data.network.PushApiService
import tw.futuremedialab.mycall.data.network.dto.ApiErrorDto
import tw.futuremedialab.mycall.data.network.dto.DeviceApproveRequestDto
import tw.futuremedialab.mycall.data.network.dto.FraudReportRequestDto
import tw.futuremedialab.mycall.data.network.dto.LoginRequestDto
import tw.futuremedialab.mycall.data.network.dto.PushSubscribeRequestDto
import tw.futuremedialab.mycall.data.network.dto.RegisterRequestDto
import tw.futuremedialab.mycall.domain.entity.UserProfile
import tw.futuremedialab.mycall.domain.repo.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val pushApiService: PushApiService,
    private val fraudApiService: FraudApiService,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<String> = runCatching {
        val response = authApiService.login(LoginRequestDto(email, password))
        if (response.isSuccessful) {
            response.body()?.accessToken ?: error("Empty response body")
        } else {
            val msg = if (response.code() == 401) "Incorrect email or password" else "Login failed (${response.code()})"
            error(msg)
        }
    }

    override suspend fun register(
        email: String,
        phoneNumber: String,
        name: String,
        password: String
    ): Result<UserProfile> = runCatching {
        val response = authApiService.register(RegisterRequestDto(email, phoneNumber, name, password))
        if (response.isSuccessful) {
            val dto = response.body() ?: error("Empty response body")
            UserProfile(dto.uuid, dto.email, dto.phoneNumber, dto.name)
        } else {
            val detail = parseErrorDetail(response.errorBody()?.string())
            error(detail ?: "Registration failed (${response.code()})")
        }
    }

    override suspend fun getAuthStatus(token: String): Result<UserProfile> = runCatching {
        val response = authApiService.getStatus("Bearer $token")
        if (response.isSuccessful) {
            val dto = response.body() ?: error("Empty response body")
            UserProfile(dto.uuid, dto.email, dto.phoneNumber, dto.name)
        } else {
            error("Unauthorized")
        }
    }

    override suspend fun subscribePush(token: String, fcmToken: String): Result<Unit> = runCatching {
        val response = pushApiService.subscribe(
            "Bearer $token",
            PushSubscribeRequestDto(platform = "fcm", fcmToken = fcmToken)
        )
        if (!response.isSuccessful) error("Push subscription failed (${response.code()})")
    }

    override suspend fun reportIncomingCall(token: String, phoneNumber: String, callerName: String?): Result<Unit> = runCatching {
        val response = fraudApiService.reportIncomingCall(
            "Bearer $token",
            FraudReportRequestDto(phoneNumber = phoneNumber, callerName = callerName)
        )
        if (!response.isSuccessful) error("Call report failed (${response.code()})")
    }

    override suspend fun reportCallEnded(token: String): Result<Unit> = runCatching {
        val response = fraudApiService.callEnd("Bearer $token")
        if (!response.isSuccessful) error("Call end report failed (${response.code()})")
    }

    override suspend fun approveDevicePairing(token: String, pairingCode: String): Result<Unit> = runCatching {
        val response = authApiService.approveDevice(
            "Bearer $token",
            DeviceApproveRequestDto(pairingCode = pairingCode)
        )
        if (!response.isSuccessful) {
            val detail = parseErrorDetail(response.errorBody()?.string())
            val fallback = when (response.code()) {
                401 -> "You need to be logged in to pair a device."
                404 -> "This QR code has expired or was already used."
                else -> "Pairing failed (${response.code()})"
            }
            error(detail ?: fallback)
        }
    }

    private fun parseErrorDetail(body: String?): String? = try {
        body?.let { Gson().fromJson(it, ApiErrorDto::class.java)?.detail }
    } catch (_: Exception) {
        body
    }
}
