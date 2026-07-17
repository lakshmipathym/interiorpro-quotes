package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.core.backup.*
import com.example.core.device.*
import com.example.core.drive.*
import com.example.core.security.*
import com.example.core.sync.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleDriveSyncTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: QuotesRepository
    
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var checksumManager: ChecksumManager
    private lateinit var integrityValidator: IntegrityValidator
    
    private lateinit var deviceManager: DeviceManager
    private lateinit var signInManager: GoogleSignInManager
    private lateinit var driveService: GoogleDriveService
    
    private lateinit var backupManager: BackupManager
    private lateinit var restoreManager: RestoreManager
    private lateinit var syncCoordinator: SyncCoordinator
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        db = AppDatabase.getDatabase(context)
        repository = QuotesRepository(db)
        
        encryptionManager = EncryptionManagerImpl()
        checksumManager = ChecksumManagerImpl()
        integrityValidator = IntegrityValidatorImpl(checksumManager)
        
        deviceManager = DeviceManagerImpl(context)
        signInManager = GoogleSignInManagerImpl(context)
        driveService = GoogleDriveServiceImpl(context, signInManager)
        
        backupManager = BackupManagerImpl(db, repository, encryptionManager, checksumManager, deviceManager)
        restoreManager = RestoreManagerImpl(context, db, repository, encryptionManager, checksumManager, integrityValidator)
        syncCoordinator = SyncCoordinatorImpl()
        
        syncManager = SyncManagerImpl(
            context = context,
            driveService = driveService,
            backupManager = backupManager,
            restoreManager = restoreManager,
            deviceManager = deviceManager,
            syncCoordinator = syncCoordinator,
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )
    }



    @Test
    fun testDeviceRegistrationMetadata() {
        // Step 5 verification: Device manager captures critical metrics correctly
        assertNotNull(deviceManager.getDeviceId())
        assertNotNull(deviceManager.getDeviceName())
        assertNotNull(deviceManager.getDeviceModel())
        assertNotNull(deviceManager.getAndroidVersion())
        assertNotNull(deviceManager.getAppVersion())
        assertTrue(deviceManager.getDatabaseVersion() > 0)
        assertTrue(deviceManager.getRegistrationTime() > 0L)
    }

    @Test
    fun testPrintFingerprint() {
        println("FINGERPRINT_VALUE: " + android.os.Build.FINGERPRINT)
        println("ROBOLECTRIC_SYSTEM_PROP: " + System.getProperty("robolectric.class"))
    }

    @Test
    fun testAuthenticationFallbackAndSignOut() {
        runBlocking {
            // Step 1 verification: Auth flow triggers correctly
            signInManager.signOut()
            assertFalse(signInManager.isUserSignedIn.value)
            assertNull(signInManager.currentUserEmail.value)

            // Silent sign-in should return false if no account is authorized yet
            val silentOk = signInManager.silentSignIn()
            if (!silentOk) {
                val interactiveOk = signInManager.signIn(context)
                assertTrue(interactiveOk)
                assertTrue(signInManager.isUserSignedIn.value)
                assertEquals("sandbox.user@interiorpro.tech", signInManager.currentUserEmail.value)
            }
        }
    }

    @Test
    fun testBackupUploadAndVerifyEngine() {
        runBlocking {
            // Step 3 verification: Create -> Compress -> Encrypt -> Checksum -> Metadata -> Upload
            val cacheDir = context.cacheDir
            val backupFile = File(cacheDir, "test_backup_upload.bin")
            if (backupFile.exists()) backupFile.delete()

            // 1. Authentication
            signInManager.signIn(context)
            assertTrue(signInManager.isUserSignedIn.value)

            // 2. Generate secure archive via BackupManager
            val result = backupManager.createBackup(
                destinationFile = backupFile,
                password = "testSecretPassword123",
                encrypt = true,
                compress = true
            )

            assertTrue(result is BackupResult.Success)
            val success = result as BackupResult.Success
            
            // Assert archive exists and is non-empty
            assertTrue(backupFile.exists())
            assertTrue(backupFile.length() > 0)

            // Validate generated metadata metrics
            assertNotNull(success.metadata.checksum)
            assertTrue(success.metadata.timestamp > 0)
            assertEquals(deviceManager.getDeviceId(), success.metadata.deviceId)
            assertTrue(success.metadata.isEncrypted)
            assertTrue(success.metadata.isCompressed)

            // Clean up
            backupFile.delete()
        }
    }

    @Test
    fun testDownloadAndVerifyEngine() {
        runBlocking {
            // Step 4 verification: Locate -> Download -> Verify Checksum -> Decrypt -> Temporary Restore -> Validate
            val cacheDir = context.cacheDir
            val backupFile = File(cacheDir, "test_backup_src.bin")
            val restoredFile = File(cacheDir, "test_backup_dest.bin")
            if (backupFile.exists()) backupFile.delete()
            if (restoredFile.exists()) restoredFile.delete()

            // 1. Auth & Create Backup
            signInManager.signIn(context)
            val backupResult = backupManager.createBackup(
                destinationFile = backupFile,
                password = "securePasswordTest",
                encrypt = true,
                compress = true
            )
            assertTrue(backupResult is BackupResult.Success)

            // 2. Upload to sandbox Google Drive App Data Space
            val mimeType = "application/octet-stream"
            val metadata = mapOf("checksum" to (backupResult as BackupResult.Success).metadata.checksum)
            val fileId = driveService.uploadToAppData(backupFile, mimeType, metadata)
            assertFalse(fileId.isEmpty())

            // 3. Download back from App Data Folder Space
            val downloadOk = driveService.downloadFromAppData(fileId, restoredFile)
            assertTrue(downloadOk)
            assertTrue(restoredFile.exists())

            // 4. Staging validation (Verify SHA-256, decrypt, decompress, validate schema)
            val integrityOk = restoreManager.verifyBackupIntegrity(restoredFile, "securePasswordTest")
            assertTrue(integrityOk)

            val restoreResult = restoreManager.safeRestore(restoredFile, "securePasswordTest")
            assertTrue(restoreResult is RestoreResult.Success)

            // Clean up
            backupFile.delete()
            restoredFile.delete()
            driveService.deleteFile(fileId)
        }
    }

    @Test
    fun testEndToEndSyncOrchestration() {
        runBlocking {
            // Step 6 & 8 verification: State updates & Atomic full sync orchestration
            signInManager.signIn(context)
            assertTrue(driveService.isAuthorized())

            // Initialize state check
            assertEquals(SyncState.Idle, syncManager.syncState.value)

            // Execute coordinated atomic transfer
            val syncResult = syncManager.triggerSync()
            
            // Assert sync reports success
            assertTrue(syncResult is SyncResult.Success)
            
            // Assert state updated to Success State with timestamp
            assertTrue(syncManager.syncState.value is SyncState.Success)
        }
    }

    @Test
    fun testWorkManagerAutoBackupOnSave() {
        // Step 1: Set policy to ON_SAVE
        syncManager.setAutoBackupPolicy("ON_SAVE")
        assertEquals("ON_SAVE", syncManager.getAutoBackupPolicy())

        // Step 2: Trigger onQuotationSaved()
        syncManager.onQuotationSaved()

        // Step 3: Verify that the unique work "on_save_backup" is enqueued in WorkManager
        val workManager = androidx.work.WorkManager.getInstance(context)
        var workInfos: List<androidx.work.WorkInfo> = emptyList()
        for (i in 1..50) {
            workInfos = workManager.getWorkInfosForUniqueWork("on_save_backup").get()
            if (workInfos.isNotEmpty()) break
            Thread.sleep(20)
        }
        assertNotNull(workInfos)
        assertTrue(workInfos.isNotEmpty())
        val workInfo = workInfos[0]
        assertEquals(androidx.work.WorkInfo.State.ENQUEUED, workInfo.state)
    }

    @Test
    fun testWorkManagerPeriodicPolicyScheduling() {
        // Step 1: Set policy to DAILY
        syncManager.setAutoBackupPolicy("DAILY")
        assertEquals("DAILY", syncManager.getAutoBackupPolicy())

        // Step 2: Verify periodic_policy_backup is scheduled
        val workManager = androidx.work.WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork("periodic_policy_backup").get()
        assertNotNull(workInfos)
        assertTrue(workInfos.isNotEmpty())
        val workInfo = workInfos[0]
        assertNotNull(workInfo)

        // Step 3: Change policy to MANUAL
        syncManager.setAutoBackupPolicy("MANUAL")
        assertEquals("MANUAL", syncManager.getAutoBackupPolicy())

        // Step 4: Verify periodic_policy_backup is cancelled
        val cancelledWorkInfos = workManager.getWorkInfosForUniqueWork("periodic_policy_backup").get()
        assertTrue(cancelledWorkInfos.isEmpty() || cancelledWorkInfos[0].state == androidx.work.WorkInfo.State.CANCELLED)
    }

    @Test
    fun testAutoRestoreEngineSafeStagingValidation() = kotlinx.coroutines.test.runTest {
        // Step 1: Pre-populate local database with stable data
        val profile = com.example.data.CompanyProfile(
            id = 1,
            companyName = "Initial Valid Company",
            phone = "1234567890",
            email = "company@email.com",
            gstin = "12345",
            termsAndConditions = "Terms"
        )
        db.companyProfileDao().insertOrUpdate(profile)

        // Step 2: Create a backup of this valid state
        val backupFile = File(context.cacheDir, "valid_test_backup.bin")
        val backupResult = backupManager.createBackup(backupFile, "securePasswordTest")
        assertTrue(backupResult is com.example.core.backup.BackupResult.Success)

        // Verify the backup can be successfully restored using safeRestore
        val initialRestoreResult = restoreManager.safeRestore(backupFile, "securePasswordTest")
        assertTrue(initialRestoreResult is RestoreResult.Success)

        // Step 3: Create a corrupt backup file (with invalid structural schema but correct encryption)
        val badJsonText = "{\"backup_version\": 1, \"something_else\": \"corrupted_data\"}"
        val compressedBadBytes = java.io.ByteArrayOutputStream().use { bos ->
            java.util.zip.GZIPOutputStream(bos).use { gzos ->
                gzos.write(badJsonText.toByteArray(Charsets.UTF_8))
            }
            bos.toByteArray()
        }
        val encryptedBadBytes = encryptionManager.encrypt(compressedBadBytes, "securePasswordTest")
        val corruptBackupFile = File(context.cacheDir, "corrupt_test_backup.bin")
        corruptBackupFile.writeBytes(encryptedBadBytes)

        // Step 4: Try to restore this corrupted backup
        val corruptRestoreResult = restoreManager.safeRestore(corruptBackupFile, "securePasswordTest")
        
        // Assert that validation failed and didn't result in success
        assertTrue(corruptRestoreResult is RestoreResult.InvalidBackup)

        // Step 5: Verify that the local database remains UNTOUCHED and still contains the initial profile!
        val currentProfile = db.companyProfileDao().getProfileDirect()
        assertNotNull(currentProfile)
        assertEquals("Initial Valid Company", currentProfile?.companyName)
    }
}
