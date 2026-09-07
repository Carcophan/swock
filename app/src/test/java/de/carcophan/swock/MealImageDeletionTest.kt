package de.carcophan.swock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MealImageDeletionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDirectImageFileDeletion() {
        val imagesDir = tempFolder.newFolder("food_images")
        val imageFile = File(imagesDir, "food_12345.jpg").apply {
            writeText("dummy image bytes")
        }
        assertTrue("Test file should exist before deletion", imageFile.exists())

        // Simulate deletion logic
        val cleanPath = imageFile.absolutePath.removePrefix("file://")
        val target = File(cleanPath)
        if (target.exists()) {
            target.delete()
        }

        assertFalse("File should be deleted", imageFile.exists())
    }

    @Test
    fun testFileUriPrefixDeletion() {
        val imagesDir = tempFolder.newFolder("food_images")
        val imageFile = File(imagesDir, "food_prefix.jpg").apply {
            writeText("dummy image bytes")
        }
        assertTrue("Test file should exist before deletion", imageFile.exists())

        val uriPath = "file://${imageFile.absolutePath}"
        val cleanPath = if (uriPath.startsWith("file://")) uriPath.removePrefix("file://") else uriPath
        val target = File(cleanPath)
        if (target.exists()) {
            target.delete()
        }

        assertFalse("File should be deleted even when path had file:// prefix", imageFile.exists())
    }

    @Test
    fun testOrphanedImagesCleanupLogic() {
        val imagesDir = tempFolder.newFolder("food_images")

        val activeMealImage = File(imagesDir, "active_meal.jpg").apply { writeText("active image") }
        val orphanedImage1 = File(imagesDir, "orphaned_1.jpg").apply { writeText("orphaned image 1") }
        val orphanedImage2 = File(imagesDir, "orphaned_2.png").apply { writeText("orphaned image 2") }

        val activeMealPaths = setOf(activeMealImage.absolutePath)
        val activeMealFileNames = activeMealPaths.map { File(it).name }.toSet()

        // Simulate cleanOrphanedImages logic
        imagesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val isReferenced = activeMealFileNames.contains(file.name) || activeMealPaths.contains(file.absolutePath)
                if (!isReferenced) {
                    file.delete()
                }
            }
        }

        assertTrue("Active meal image must NOT be deleted", activeMealImage.exists())
        assertFalse("Orphaned image 1 must be deleted", orphanedImage1.exists())
        assertFalse("Orphaned image 2 must be deleted", orphanedImage2.exists())
    }

    @Test
    fun testOrphanedImagesWithExclusion() {
        val imagesDir = tempFolder.newFolder("food_images")

        val activeImage = File(imagesDir, "active.jpg").apply { writeText("active") }
        val pendingAnalysisImage = File(imagesDir, "pending.jpg").apply { writeText("pending confirmation") }
        val deadOrphanImage = File(imagesDir, "dead_orphan.jpg").apply { writeText("dead") }

        val activeMealPaths = setOf(activeImage.absolutePath)
        val activeMealFileNames = activeMealPaths.map { File(it).name }.toSet()
        val excludedPaths = setOf(pendingAnalysisImage.absolutePath)

        imagesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val isReferenced = activeMealFileNames.contains(file.name) ||
                        activeMealPaths.contains(file.absolutePath) ||
                        excludedPaths.contains(file.absolutePath) ||
                        excludedPaths.any { it.endsWith(file.name) }
                if (!isReferenced) {
                    file.delete()
                }
            }
        }

        assertTrue("Active image must be preserved", activeImage.exists())
        assertTrue("Pending analysis image must be preserved", pendingAnalysisImage.exists())
        assertFalse("Dead orphan must be deleted", deadOrphanImage.exists())
    }
}
