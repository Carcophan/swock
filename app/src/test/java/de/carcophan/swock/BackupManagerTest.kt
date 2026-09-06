package de.carcophan.swock

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testMetadataJsonSerializationAndParsing() {
        val json = JSONObject().apply {
            put("version", 1)
            put("appVersion", "1.0")
            put("createdAt", 1700000000000L)
            put("mealCount", 42)
            put("imageCount", 12)
            put("appId", "de.carcophan.swock")
        }.toString(2)

        val parsed = JSONObject(json)
        assertEquals(1, parsed.getInt("version"))
        assertEquals("1.0", parsed.getString("appVersion"))
        assertEquals(1700000000000L, parsed.getLong("createdAt"))
        assertEquals(42, parsed.getInt("mealCount"))
        assertEquals(12, parsed.getInt("imageCount"))
        assertEquals("de.carcophan.swock", parsed.getString("appId"))
    }

    @Test
    fun testZipCreationAndExtractionIntegrity() {
        val zipFile = tempFolder.newFile("test_backup.zip")
        val extractDir = tempFolder.newFolder("extracted")

        val dummyDbContent = "SQLite format 3\u0000dummy_database_data"
        val dummyProfileContent = "{\"geminiModel\":\"gemini-3.6-flash\",\"age\":28}"
        val dummyImage1Content = "JPEG_DATA_12345"
        val dummyImage2Content = "JPEG_DATA_67890"

        // Create Zip
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // Entry 1: metadata.json
            zos.putNextEntry(ZipEntry("metadata.json"))
            zos.write("{\"mealCount\":2,\"imageCount\":2}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entry 2: database/swock_meals.db
            zos.putNextEntry(ZipEntry("database/swock_meals.db"))
            zos.write(dummyDbContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entry 3: preferences/user_profile.json
            zos.putNextEntry(ZipEntry("preferences/user_profile.json"))
            zos.write(dummyProfileContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entry 4: food_images/food_1.jpg
            zos.putNextEntry(ZipEntry("food_images/food_1.jpg"))
            zos.write(dummyImage1Content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entry 5: food_images/food_2.jpg
            zos.putNextEntry(ZipEntry("food_images/food_2.jpg"))
            zos.write(dummyImage2Content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)

        // Extract Zip
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(extractDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Verify extracted files
        val extractedDb = File(extractDir, "database/swock_meals.db")
        assertTrue(extractedDb.exists())
        assertEquals(dummyDbContent, extractedDb.readText(Charsets.UTF_8))

        val extractedProfile = File(extractDir, "preferences/user_profile.json")
        assertTrue(extractedProfile.exists())
        assertEquals(dummyProfileContent, extractedProfile.readText(Charsets.UTF_8))

        val extractedImg1 = File(extractDir, "food_images/food_1.jpg")
        assertTrue(extractedImg1.exists())
        assertEquals(dummyImage1Content, extractedImg1.readText(Charsets.UTF_8))

        val extractedImg2 = File(extractDir, "food_images/food_2.jpg")
        assertTrue(extractedImg2.exists())
        assertEquals(dummyImage2Content, extractedImg2.readText(Charsets.UTF_8))
    }
}
