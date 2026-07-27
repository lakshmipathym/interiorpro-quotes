package com.example.core.backup.pkg

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class CompressionManager {
    fun compress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(data)
        }
        return bos.toByteArray()
    }
}
