package com.example.ui.quotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigureItemsTest {

    @Test
    fun testMaterialChangeDependency() {
        val masterData = emptyList<com.example.data.MasterEntity>()
        // 1. Particle Board Prelam behavior
        val pbGrades = getGradesForMaterial("Particle Board", masterData)
        assertTrue(pbGrades.contains("Pre-Laminated"))
        
        val pbFinishesPrelam = getFinishesForMaterial("Particle Board", "Pre-Laminated", emptyList(), masterData)
        assertTrue(pbFinishesPrelam.isEmpty())

        val pbFinishesStandard = getFinishesForMaterial("Particle Board", "Standard / Plain", emptyList(), masterData)
        assertTrue(pbFinishesStandard.contains("Laminate"))

        // 3. Particle Board thickness list
        val pbThickness = getThicknessOptionsForMaterial("Particle Board", masterData)
        assertTrue(pbThickness.containsAll(listOf("6 mm", "8 mm", "12 mm", "15 mm", "17 mm", "18 mm")))
        
        // 4. ACP Grade/Thickness separation
        val acpGrades = getGradesForMaterial("ACP", masterData)
        assertTrue(acpGrades.contains("Interior Grade") && !acpGrades.contains("3mm"))
        
        val acpThickness = getThicknessOptionsForMaterial("ACP", masterData)
        assertTrue(acpThickness.containsAll(listOf("3 mm", "4 mm", "6 mm")))

        // 5. Glass Grade/Finish separation
        val glassGrades = getGradesForMaterial("Glass", masterData)
        assertTrue(glassGrades.contains("Toughened") && !glassGrades.contains("Etched"))
        
        val glassFinishes = getFinishesForMaterial("Glass", "Toughened", emptyList(), masterData)
        assertTrue(glassFinishes.contains("Etched") && !glassFinishes.contains("Toughened"))

        // 6. WPC / PVC Board / Blockboard rule
        val wpcGrades = getGradesForMaterial("WPC", masterData)
        assertTrue(wpcGrades.contains("High Density"))
        
        val pvcFinishes = getFinishesForMaterial("PVC Board", "Standard", emptyList(), masterData)
        assertTrue(pvcFinishes.contains("PVC Laminate"))
        
        val bbThickness = getThicknessOptionsForMaterial("Blockboard", masterData)
        assertTrue(bbThickness.containsAll(listOf("19 mm", "25 mm")))
    }
}
