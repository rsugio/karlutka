package karlutka.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.forEachDirectoryEntry

object KTempFile {
    //по умолчанию
    var tempFolder: Path = Paths.get(System.getProperty("java.io.tmpdir") + "/karlutka2")
    fun start() {
        if (Files.isDirectory(tempFolder)) {
            tempFolder.forEachDirectoryEntry {
                Files.delete(it)
            }
        } else {
            //TODO для линупса прописать права rw-rw-r--
            Files.createDirectory(tempFolder)
        }

    }

    fun getTempFileXiObj() = Files.createTempFile(tempFolder, "xiobj_", ".xml")
    fun getTempFileTpt() = Files.createTempFile(tempFolder, "tpt_", ".bin")
    fun getTempFileZip() = Files.createTempFile(tempFolder, "zip_", ".zip")

    fun task(): Path {
        val path: Path = Files.createTempFile(tempFolder, "task", ".xml")
        // может быть какая-то логика здесь
        return path
    }

    fun delete(p: Path) {
        // может быть какая-то логика здесь
        Files.delete(p)
    }
}