cat << 'INNER_EOF' > app/src/test/java/com/example/data/BrandingAssetCopierImplTest.kt
package com.example.data

import android.content.Context
import com.example.domain.models.CompanySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BrandingAssetCopierImplTest {

    private lateinit var context: Context
    private lateinit var testFilesDir: File
    private lateinit var copier: BrandingAssetCopierImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testFilesDir = context.filesDir
        copier = BrandingAssetCopierImpl(context)
    }

    @Test
    fun `test copies assets correctly`() = runBlocking {
        val originalLogo = File(testFilesDir, "original_logo.png")
        originalLogo.writeText("logo data")
        val originalSig = File(testFilesDir, "original_sig.png")
        originalSig.writeText("sig data")

        val company = CompanySnapshot(
            companyName = "Test",
            ownerName = "",
            phone = "",
            email = "",
            address = "",
            bankName = "",
            accountHolderName = "",
            accountNumber = "",
            ifsc = "",
            branch = "",
            upiId = "",
            logoPath = originalLogo.absolutePath,
            signaturePath = originalSig.absolutePath,
            companySealPath = "" // missing
        )

        val updated = copier.copyAssetsForQuotation("Q-1", company)

        assertNotEquals(originalLogo.absolutePath, updated.logoPath)
        assertTrue(File(updated.logoPath).exists())
        assertEquals("logo data", File(updated.logoPath).readText())

        assertNotEquals(originalSig.absolutePath, updated.signaturePath)
        assertTrue(File(updated.signaturePath).exists())
        assertEquals("sig data", File(updated.signaturePath).readText())

        assertEquals("", updated.companySealPath)
    }

    @Test
    fun `test duplicate prevention does not overwrite existing assets`() = runBlocking {
        val originalLogo = File(testFilesDir, "original_logo.png")
        originalLogo.writeText("new logo data")

        val quoteDir = File(testFilesDir, "quotation_assets/Q-2")
        quoteDir.mkdirs()
        val existingLogo = File(quoteDir, "logo.png")
        existingLogo.writeText("old logo data")

        val company = CompanySnapshot(
            companyName = "Test",
            ownerName = "",
            phone = "",
            email = "",
            address = "",
            bankName = "",
            accountHolderName = "",
            accountNumber = "",
            ifsc = "",
            branch = "",
            upiId = "",
            logoPath = originalLogo.absolutePath,
            signaturePath = "",
            companySealPath = ""
        )

        val updated = copier.copyAssetsForQuotation("Q-2", company)

        assertEquals(existingLogo.absolutePath, updated.logoPath)
        assertEquals("old logo data", File(updated.logoPath).readText()) // Was not overwritten
    }

    @Test
    fun `test missing source files gracefully fallback to empty`() = runBlocking {
        val company = CompanySnapshot(
            companyName = "Test",
            ownerName = "",
            phone = "",
            email = "",
            address = "",
            bankName = "",
            accountHolderName = "",
            accountNumber = "",
            ifsc = "",
            branch = "",
            upiId = "",
            logoPath = "/does/not/exist/logo.png",
            signaturePath = "",
            companySealPath = ""
        )

        val updated = copier.copyAssetsForQuotation("Q-3", company)

        assertEquals("", updated.logoPath)
        assertEquals("", updated.signaturePath)
    }
}
INNER_EOF
