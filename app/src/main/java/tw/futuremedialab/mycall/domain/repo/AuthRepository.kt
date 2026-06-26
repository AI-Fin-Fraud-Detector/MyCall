package tw.futuremedialab.mycall.domain.repo

import tw.futuremedialab.mycall.domain.entity.UserProfile

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>
    suspend fun register(email: String, phoneNumber: String, name: String, password: String): Result<UserProfile>
    suspend fun getAuthStatus(token: String): Result<UserProfile>
    suspend fun subscribePush(token: String, fcmToken: String): Result<Unit>
    suspend fun reportIncomingCall(token: String, phoneNumber: String, callerName: String? = null): Result<Unit>
    suspend fun reportCallEnded(token: String): Result<Unit>

    suspend fun approveDevicePairing(token: String, pairingCode: String): Result<Unit>
}
