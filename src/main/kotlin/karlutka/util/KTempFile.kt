package karlutka.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object KTempFile {
    //по умолчанию
    var tempFolder: Path = Paths.get(System.getProperty("java.io.tmpdir") + "/karlutka2")

    fun task(): Path {
        val path: Path = Files.createTempFile(tempFolder, "task", ".xml")
        // может быть какая-то логика здесь
        return path
    }
    fun delete(p:Path) {
        // может быть какая-то логика здесь
        Files.delete(p)
    }
}