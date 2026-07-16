package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("InteriorPro Quotes", appName)
  }

  @Test
  fun `test master database operations and duplicate checking`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val masterRepo = MasterRepository(database)

    // Verify seeded project types exist
    val projects = masterRepo.getMastersByTypeDirect("PROJECT_TYPE")
    assertTrue(projects.isNotEmpty())
    assertTrue(projects.any { it.name == "Modular Kitchen" })

    // Save a new custom Category master record
    val originalCount = masterRepo.getMastersByTypeDirect("CATEGORY").size
    val newMasterId = masterRepo.saveMaster(
      MasterEntity(
        masterType = "CATEGORY",
        name = "Premium Decorative Panels",
        description = "High density fiberboard panels",
        displayOrder = 99
      )
    )
    assertTrue(newMasterId > 0)

    val updatedCount = masterRepo.getMastersByTypeDirect("CATEGORY").size
    assertEquals(originalCount + 1, updatedCount)

    // Test duplicate name prevention check ignoring case
    val duplicate = masterRepo.getMasterByTypeAndName("CATEGORY", " premium decorative panels ")
    assertNotNull(duplicate)
    assertEquals("Premium Decorative Panels", duplicate?.name)

    // Test soft deletion
    masterRepo.softDeleteMaster(newMasterId)
    val afterDeleteCount = masterRepo.getMastersByTypeDirect("CATEGORY").size
    assertEquals(originalCount, afterDeleteCount)
  }
}

