package com.arifmusic.app.data.repository

import android.util.Log
import com.arifmusic.app.data.local.UserDao
import com.arifmusic.app.data.model.User
import com.arifmusic.app.data.model.UserType
import com.arifmusic.app.data.model.VerificationStatus
import com.arifmusic.app.data.remote.ApiService
import com.arifmusic.app.data.remote.LoginRequest
import com.arifmusic.app.data.remote.UserRegistrationRequest
import com.arifmusic.app.data.remote.UserUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val apiService: ApiService,
    private val connectivityRepository: ConnectivityRepository
) {

    suspend fun registerUser(
        fullName: String,
        name: String,
        email: String,
        password: String,
        userType: UserType
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (isUserExists(email)) {
                Log.d("UserRepository", "User with email $email already exists")
                return@withContext Result.failure(Exception("User with this email already exists"))
            }

            val hashedPassword = hashPassword(password)
            val userId = generateUserId()

            val isConnected = connectivityRepository.isNetworkAvailable.value
            Log.d("UserRepository", "Network available for registration: $isConnected")

            if (isConnected) {
                try {
                    Log.d("UserRepository", "Attempting API registration for $email")
                    val response = apiService.registerUser(
                        UserRegistrationRequest(
                            id = userId,
                            email = email,
                            password = password,
                            name = name,
                            fullName = fullName,
                            userType = userType.name
                        )
                    )

                    if (response.isSuccessful) {
                        val userResponse = response.body()
                        if (userResponse != null) {
                            Log.d("UserRepository", "API registration successful for $email")

                            sessionManager.saveToken(userResponse.token)

                            val user = User(
                                id = userResponse.id,
                                email = email,
                                password = hashedPassword,
                                name = name,
                                fullName = fullName,
                                userType = userType,
                                verificationStatus = VerificationStatus.UNVERIFIED,
                                socialLinks = emptyMap()
                            )
                            userDao.insertUser(user)
                            return@withContext Result.success(user)
                        } else {
                            Log.e("UserRepository", "API registration response body is null")
                            return@withContext Result.failure(Exception("Failed to parse user response"))
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("UserRepository", "API registration failed: $errorBody")

                        return@withContext fallbackToLocalRegistration(userId, email, hashedPassword, name, fullName, userType)
                    }
                } catch (e: Exception) {
                    Log.e("UserRepository", "API registration exception: ${e.message}", e)
                    return@withContext fallbackToLocalRegistration(userId, email, hashedPassword, name, fullName, userType)
                }
            } else {
                Log.d("UserRepository", "No network, using local registration for $email")
                return@withContext fallbackToLocalRegistration(userId, email, hashedPassword, name, fullName, userType)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Registration exception: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    private suspend fun fallbackToLocalRegistration(
        userId: String,
        email: String,
        hashedPassword: String,
        name: String,
        fullName: String,
        userType: UserType
    ): Result<User> {
        Log.d("UserRepository", "Falling back to local registration for $email")
        val user = User(
            id = userId,
            email = email,
            password = hashedPassword,
            name = name,
            fullName = fullName,
            userType = userType,
            verificationStatus = VerificationStatus.UNVERIFIED,
            socialLinks = emptyMap()
        )
        userDao.insertUser(user)
        return Result.success(user)
    }

    suspend fun loginUser(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val isConnected = connectivityRepository.isNetworkAvailable.value
            Log.d("UserRepository", "Network available for login: $isConnected")

            if (isConnected) {
                try {
                    Log.d("UserRepository", "Attempting API login for $email")
                    val response = apiService.loginUser(
                        LoginRequest(
                            email = email,
                            password = password
                        )
                    )

                    if (response.isSuccessful) {
                        val userResponse = response.body()
                        if (userResponse != null) {
                            Log.d("UserRepository", "API login successful for $email")

                            sessionManager.saveToken(userResponse.token)

                            var user = userDao.getUserByEmail(email)
                            if (user == null) {
                                user = User(
                                    id = userResponse.id,
                                    email = email,
                                    password = hashPassword(password),
                                    name = userResponse.name,
                                    fullName = userResponse.fullName,
                                    userType = UserType.valueOf(userResponse.userType),
                                    verificationStatus = VerificationStatus.UNVERIFIED,
                                    socialLinks = emptyMap()
                                )
                                userDao.insertUser(user)
                            } else {
                                user = user.copy(
                                    id = userResponse.id,
                                    name = userResponse.name,
                                    fullName = userResponse.fullName,
                                    userType = UserType.valueOf(userResponse.userType)
                                )
                                userDao.insertUser(user)
                            }
                            return@withContext Result.success(user)
                        } else {
                            Log.e("UserRepository", "API login response body is null")
                            return@withContext Result.failure(Exception("Failed to parse user response"))
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("UserRepository", "API login failed: $errorBody")

                        return@withContext fallbackToLocalLogin(email, password)
                    }
                } catch (e: Exception) {
                    Log.e("UserRepository", "API login exception: ${e.message}", e)
                    return@withContext fallbackToLocalLogin(email, password)
                }
            } else {
                Log.d("UserRepository", "No network, using local login for $email")
                return@withContext fallbackToLocalLogin(email, password)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Login exception: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    private suspend fun fallbackToLocalLogin(email: String, password: String): Result<User> {
        Log.d("UserRepository", "Falling back to local login for $email")
        val hashedPassword = hashPassword(password)
        val user = userDao.getUserByEmail(email)

        return if (user != null && user.password == hashedPassword) {
            Log.d("UserRepository", "Local login successful for $email")
            Result.success(user)
        } else {
            Log.e("UserRepository", "Local login failed for $email: Invalid credentials")
            Result.failure(Exception("Invalid credentials"))
        }
    }

    suspend fun deleteAccount(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isConnected = connectivityRepository.isNetworkAvailable.value

            if (isConnected) {
                try {
                    Log.d("UserRepository", "Attempting API delete for user $userId")
                    val response = apiService.deleteUser(userId)

                    if (response.isSuccessful) {
                        Log.d("UserRepository", "API delete successful for user $userId")
                        userDao.deleteUserById(userId)
                        return@withContext Result.success(Unit)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("UserRepository", "API delete failed: $errorBody")
                        userDao.deleteUserById(userId)
                        return@withContext Result.success(Unit)
                    }
                } catch (e: Exception) {
                    Log.e("UserRepository", "API delete exception: ${e.message}", e)
                    userDao.deleteUserById(userId)
                    return@withContext Result.success(Unit)
                }
            } else {
                Log.d("UserRepository", "No network, deleting user $userId locally")
                userDao.deleteUserById(userId)
                return@withContext Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Delete account exception: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }



    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    fun getArtists(): Flow<List<User>> {
        return userDao.getUsersByType(UserType.ARTIST)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun getUserById(userId: String): User? {
        try {
            val response = apiService.getUserById(userId)

            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    // Update local user
                    userDao.insertUser(user)
                    return user
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "API getUserById failed: ${e.message}", e)
        }

        return userDao.getUserById(userId)
    }

    suspend fun promoteUserToArtist(userId: String) {
        userDao.updateUserType(userId, "ARTIST")
    }

    suspend fun updateUserPassword(email: String, newPassword: String): Result<User> {
        return try {
            try {
                val response = apiService.updateUserProfile(
                    UserUpdateRequest(
                        password = newPassword
                    )
                )

                if (response.isSuccessful) {
                    val updatedUser = response.body()
                    if (updatedUser != null) {
                        val localUser = userDao.getUserByEmail(email)
                        if (localUser != null) {
                            val hashedPassword = hashPassword(newPassword)
                            val localUpdatedUser = localUser.copy(password = hashedPassword)
                            userDao.insertUser(localUpdatedUser)
                            return Result.success(localUpdatedUser)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "API updateUserPassword failed: ${e.message}", e)
            }

            val user = userDao.getUserByEmail(email) ?: return Result.failure(Exception("User not found"))
            val hashedPassword = hashPassword(newPassword)
            val updatedUser = user.copy(password = hashedPassword)
            userDao.insertUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(email: String, fullName: String, bio: String?): Result<User> {
        return try {
            try {
                val response = apiService.updateUserProfile(
                    UserUpdateRequest(
                        fullName = fullName,
                        bio = bio
                    )
                )

                if (response.isSuccessful) {
                    val updatedUser = response.body()
                    if (updatedUser != null) {
                        // Update local user
                        userDao.insertUser(updatedUser)
                        return Result.success(updatedUser)
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "API updateUserProfile failed: ${e.message}", e)
            }

            val user = userDao.getUserByEmail(email) ?: return Result.failure(Exception("User not found"))
            val updatedUser = user.copy(fullName = fullName, bio = bio)
            userDao.insertUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileImage(email: String, imageUrl: String): Result<Unit> {
        return try {
            try {
                val response = apiService.updateUserProfile(
                    UserUpdateRequest(
                        profileImageUrl = imageUrl
                    )
                )

                if (response.isSuccessful) {
                    // Update local user
                    userDao.updateProfileImage(email, imageUrl)
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "API updateProfileImage failed: ${e.message}", e)
            }

            // Fall back to local
            userDao.updateProfileImage(email, imageUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Other methods remain the same...
    suspend fun approveArtist(email: String, approved: Boolean): Result<Unit> {
        return try {
            userDao.updateApprovalStatus(email, approved)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUserExists(email: String): Boolean {
        val user = userDao.getUserByEmail(email)
        return user != null
    }



    suspend fun updateArtistApprovalStatus(artistId: String, approved: Boolean) {
        try {
            userDao.updateArtistApprovalStatus(artistId, approved)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun suspendUser(userId: String, isSuspended: Boolean = true): Result<Unit> {
        return try {
            userDao.updateUserSuspension(userId, isSuspended)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateUserId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    fun getUsersAwaitingVerification(): Flow<List<User>> {
        return userDao.getUsersByType(UserType.ARTIST).map { users ->
            users.filter { it.verificationStatus == VerificationStatus.PENDING }
        }
    }

    suspend fun promoteUserToAdmin(userId: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
            val updatedUser = user.copy(userType = UserType.ADMIN)
            userDao.insertUser(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveUserVerification(userId: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
            val updatedUser = user.copy(verificationStatus = VerificationStatus.VERIFIED)
            userDao.insertUser(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectUserVerification(userId: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
            val updatedUser = user.copy(verificationStatus = VerificationStatus.REJECTED)
            userDao.insertUser(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
