package com.example.data.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.SheetRecord
import com.example.data.model.SidFeedingEntry
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data class Authenticated(val profile: AppUserProfile) : AuthUiState()
    data object Unauthenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class FirebaseManager(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val credentialManager = CredentialManager.create(context)

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _starredCases = MutableStateFlow<List<StarredCase>>(emptyList())
    val starredCases: StateFlow<List<StarredCase>> = _starredCases.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        val currentAuth = auth
        if (currentAuth == null) {
            _authState.value = AuthUiState.Unauthenticated
            return
        }

        currentAuth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val profile = AppUserProfile(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName ?: if (user.isAnonymous) "Guest User" else "Public User",
                    photoUrl = user.photoUrl?.toString(),
                    isAnonymous = user.isAnonymous,
                    isCloudConnected = true
                )
                _authState.value = AuthUiState.Authenticated(profile)
                syncUserProfile(profile)
                listenToStarredCases(user.uid)
            } else {
                _authState.value = AuthUiState.Unauthenticated
                _starredCases.value = emptyList()
            }
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    /**
     * Google Sign-In using Android Credential Manager and Firebase Auth.
     */
    suspend fun signInWithGoogle(webClientId: String = "173189729698-webclientid.apps.googleusercontent.com"): Result<AppUserProfile> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase is not initialized"))

        _authState.value = AuthUiState.Loading
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = currentAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw Exception("User data missing after Google Sign-In")

                val profile = AppUserProfile(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName ?: googleIdTokenCredential.displayName,
                    photoUrl = user.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                    isAnonymous = false,
                    isCloudConnected = true
                )
                _authState.value = AuthUiState.Authenticated(profile)
                syncUserProfile(profile)
                listenToStarredCases(user.uid)
                Result.success(profile)
            } else {
                val err = "Unexpected credential type returned"
                _authState.value = AuthUiState.Error(err)
                Result.failure(Exception(err))
            }
        } catch (e: GetCredentialCancellationException) {
            _authState.value = AuthUiState.Unauthenticated
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "Google Sign-In failed"
            _authState.value = AuthUiState.Error(errMsg)
            Result.failure(e)
        }
    }

    /**
     * Sign-In Anonymously to identify the user session securely.
     */
    suspend fun signInAnonymously(): Result<AppUserProfile> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase is not initialized"))

        _authState.value = AuthUiState.Loading
        return try {
            val authResult = currentAuth.signInAnonymously().await()
            val user = authResult.user ?: throw Exception("Anonymous user creation failed")
            val profile = AppUserProfile(
                uid = user.uid,
                email = null,
                displayName = "Public Citizen (${user.uid.take(4)})",
                photoUrl = null,
                isAnonymous = true,
                isCloudConnected = true
            )
            _authState.value = AuthUiState.Authenticated(profile)
            syncUserProfile(profile)
            listenToStarredCases(user.uid)
            Result.success(profile)
        } catch (e: Exception) {
            _authState.value = AuthUiState.Error(e.localizedMessage ?: "Anonymous sign-in failed")
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth?.signOut()
        _authState.value = AuthUiState.Unauthenticated
        _starredCases.value = emptyList()
    }

    /**
     * Cloud Firestore: Sync user profile document.
     */
    private fun syncUserProfile(profile: AppUserProfile) {
        val db = firestore ?: return
        val userDoc = mapOf(
            "uid" to profile.uid,
            "email" to profile.email,
            "displayName" to profile.displayName,
            "photoUrl" to profile.photoUrl,
            "isAnonymous" to profile.isAnonymous,
            "lastActive" to System.currentTimeMillis()
        )
        db.collection("users").document(profile.uid)
            .set(userDoc, SetOptions.merge())
    }

    /**
     * Cloud Firestore: Real-time listener for user's bookmarked / starred cases.
     */
    private fun listenToStarredCases(userId: String) {
        val db = firestore ?: return
        db.collection("users").document(userId).collection("starred_cases")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val caseNumber = doc.getString("caseNumber").orEmpty()
                    val thana = doc.getString("thana").orEmpty()
                    val thanaEn = doc.getString("thanaEn").orEmpty()
                    val sidNumber = doc.getString("sidNumber").orEmpty()
                    val registrationDate = doc.getString("registrationDate").orEmpty()
                    val isPending = doc.getBoolean("isPending") ?: false
                    val savedAt = doc.getLong("savedAt") ?: System.currentTimeMillis()
                    StarredCase(id, caseNumber, thana, thanaEn, sidNumber, registrationDate, isPending, savedAt)
                }
                _starredCases.value = list
            }
    }

    /**
     * Cloud Firestore: Star/Bookmark a case record.
     */
    suspend fun starCase(record: SheetRecord): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        val user = auth?.currentUser ?: return Result.failure(Exception("Please sign in to save cases"))

        return try {
            val docId = "case_${record.id}_${record.caseNumber.replace("/", "_")}"
            val data = mapOf(
                "id" to record.id.toString(),
                "caseNumber" to record.caseNumber,
                "thana" to record.thana,
                "thanaEn" to record.thanaEn,
                "sidNumber" to record.sidNumber,
                "registrationDate" to record.registrationDate,
                "isPending" to record.isPending,
                "savedAt" to System.currentTimeMillis()
            )
            db.collection("users").document(user.uid)
                .collection("starred_cases").document(docId)
                .set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cloud Firestore: Unstar/Remove a case record.
     */
    suspend fun unstarCase(record: SheetRecord): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        val user = auth?.currentUser ?: return Result.failure(Exception("Please sign in"))

        return try {
            val docId = "case_${record.id}_${record.caseNumber.replace("/", "_")}"
            db.collection("users").document(user.uid)
                .collection("starred_cases").document(docId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cloud Firestore: Sync user submission to Firestore cloud collection.
     */
    suspend fun submitRecordToCloud(
        thana: String,
        caseNumber: String,
        regDate: String,
        sidNumber: String,
        sidDate: String,
        remarks: String
    ): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        val user = auth?.currentUser

        return try {
            val docId = "submission_${System.currentTimeMillis()}"
            val data = mapOf(
                "id" to docId,
                "userId" to (user?.uid ?: "anonymous"),
                "userEmail" to user?.email,
                "thana" to thana,
                "caseNumber" to caseNumber,
                "registrationDate" to regDate,
                "sidNumber" to sidNumber,
                "sidCreationDate" to sidDate,
                "remarks" to remarks,
                "timestamp" to System.currentTimeMillis()
            )

            // Save in root public submissions collection for live sharing
            db.collection("public_submissions").document(docId).set(data).await()

            // If user signed in, also save under user document
            if (user != null) {
                db.collection("users").document(user.uid)
                    .collection("my_submissions").document(docId).set(data).await()
            }

            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cloud Firestore: Listen for live public submissions from other users.
     */
    fun listenToPublicSubmissions(): Flow<List<CloudSubmission>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("public_submissions")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    CloudSubmission(
                        id = doc.getString("id") ?: doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        userEmail = doc.getString("userEmail"),
                        thana = doc.getString("thana").orEmpty(),
                        caseNumber = doc.getString("caseNumber").orEmpty(),
                        registrationDate = doc.getString("registrationDate").orEmpty(),
                        sidNumber = doc.getString("sidNumber").orEmpty(),
                        sidCreationDate = doc.getString("sidCreationDate").orEmpty(),
                        remarks = doc.getString("remarks").orEmpty(),
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Cloud Firestore: Record a Thana's SID feeding event.
     */
    suspend fun recordSidFeeding(entry: SidFeedingEntry): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docId = "feed_${entry.thana}_${entry.caseNumber.replace("/", "_")}_${entry.timestamp}"
            val data = mapOf(
                "id" to docId,
                "thana" to entry.thana,
                "caseNumber" to entry.caseNumber,
                "registrationDate" to entry.registrationDate,
                "sidNumber" to entry.sidNumber,
                "sidCreationDate" to entry.sidCreationDate,
                "officerName" to entry.officerName,
                "officerRole" to entry.officerRole,
                "timestamp" to entry.timestamp
            )
            db.collection("sid_feedings").document(docId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cloud Firestore: Real-time listener for all police station SID feedings (Admin Monitor).
     */
    fun listenToSidFeedings(): Flow<List<SidFeedingEntry>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("sid_feedings")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    SidFeedingEntry(
                        id = doc.getString("id") ?: doc.id,
                        thana = doc.getString("thana").orEmpty(),
                        caseNumber = doc.getString("caseNumber").orEmpty(),
                        registrationDate = doc.getString("registrationDate").orEmpty(),
                        sidNumber = doc.getString("sidNumber").orEmpty(),
                        sidCreationDate = doc.getString("sidCreationDate").orEmpty(),
                        officerName = doc.getString("officerName").orEmpty(),
                        officerRole = doc.getString("officerRole") ?: "थाना ऑपरेटर",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }
}
