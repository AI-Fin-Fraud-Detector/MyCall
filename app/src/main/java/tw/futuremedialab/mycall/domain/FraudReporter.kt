package tw.futuremedialab.mycall.domain

import tw.futuremedialab.mycall.data.local.UserPreferences
import tw.futuremedialab.mycall.domain.repo.AuthRepository
import tw.futuremedialab.mycall.domain.repo.ContactDetailProvider
import tw.futuremedialab.mycall.util.LoggingUtil
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FraudReporter"

@Singleton
class FraudReporter @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val contactDetailProvider: ContactDetailProvider
) {
    suspend fun reportIncomingCall(phoneNumber: String) {
        val token = userPreferences.getAccessToken() ?: run {
            LoggingUtil.d(TAG, "No token — skipping fraud report")
            return
        }
        val callerName = if (phoneNumber.isNotEmpty()) {
            contactDetailProvider.getContactByPhone(phoneNumber)?.name
        } else null
        authRepository.reportIncomingCall(token, phoneNumber, callerName)
            .onFailure { LoggingUtil.w(TAG, "Failed to report incoming call: ${it.message}") }
    }

    suspend fun reportCallEnded() {
        val token = userPreferences.getAccessToken() ?: run {
            LoggingUtil.d(TAG, "No token — skipping call-end report")
            return
        }
        authRepository.reportCallEnded(token)
            .onFailure { LoggingUtil.w(TAG, "Failed to report call end: ${it.message}") }
    }
}
