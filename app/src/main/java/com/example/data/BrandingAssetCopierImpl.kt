package com.example.data

import android.content.Context
import com.example.domain.contracts.BrandingAssetCopier
import com.example.domain.models.CompanySnapshot
import java.io.File

class BrandingAssetCopierImpl(private val context: Context) : BrandingAssetCopier {
    override suspend fun copyAssetsForQuotation(quotationNumber: String, company: CompanySnapshot): CompanySnapshot {
        val assetsDir = File(context.filesDir, "quotation_assets")
        val quoteDir = File(assetsDir, quotationNumber)
        
        if (!quoteDir.exists()) {
            quoteDir.mkdirs()
        }

        val newLogoPath = copyFileSafe(company.logoPath, File(quoteDir, "logo.png"))
        val newSignaturePath = copyFileSafe(company.signaturePath, File(quoteDir, "signature.png"))
        val newSealPath = copyFileSafe(company.companySealPath, File(quoteDir, "seal.png"))

        return company.copy(
            logoPath = newLogoPath,
            signaturePath = newSignaturePath,
            companySealPath = newSealPath
        )
    }

    private fun copyFileSafe(sourcePath: String, destFile: File): String {
        if (sourcePath.isBlank()) return ""
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return ""
        
        try {
            if (!destFile.exists()) {
                sourceFile.copyTo(destFile, overwrite = false)
            }
            return destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            // If copy fails, fallback to empty to avoid crashing
            return ""
        }
    }
}
