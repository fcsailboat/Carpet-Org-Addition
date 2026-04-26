package publish

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class Metadata {
    val file: File
    val version: String
    val mcVersion: String
    val id: String
    val subtitle: String
    val gameVersions: List<String>

    constructor(file: File) {
        this.file = file
        val zip = ZipFile(file)
        zip.use { zip ->
            val entry: ZipEntry = zip.getEntry("fabric.mod.json") ?: throw IllegalArgumentException()
            val input = zip.getInputStream(entry)
            input.use { stream ->
                val json = GSON.fromJson(String(stream.readAllBytes()), JsonObject::class.java)
                this.version = json.get("version").asString
                this.id = json.get("id").asString
                val mcVersion = json.getAsJsonObject("depends").get("minecraft").asString
                this.mcVersion = this.parseMcVersion(mcVersion)
            }
            this.subtitle = "${this.id}-mc${this.mcVersion}-${this.version}"
            this.gameVersions = this.allDependGameVersions()
        }
    }

    private fun allDependGameVersions(): List<String> {
        return JarUploader.versions.stream().filter { this.match(it) }.toList().reversed()
    }

    private fun match(version: String): Boolean {
        val mcVersion = this.mcVersion
        if (mcVersion == version) {
            return true
        }
        val arr = mcVersion.split(".")
        if (arr.size == 3 && (arr[2] == "x" || arr[2] == "X" || arr[2] == "*")) {
            val regex = Regex("${arr[0]}\\.${arr[1]}(\\.\\d+)?")
            return version.matches(regex)
        } else {
            return false
        }
    }

    private fun parseMcVersion(mcVersion: String): String {
        if ("alpha." in mcVersion) {
            return mcVersion.replace("alpha.", "snapshot-")
        }
        if ("pre." in mcVersion) {
            return mcVersion.replace("pre.", "pre-")
        }
        if ("rc." in mcVersion) {
            return mcVersion.replace("rc.", "rc-")
        }
        if ("*" in mcVersion) {
            return mcVersion.replace("*", "x")
        }
        return mcVersion
    }

    companion object {
        private val GSON: Gson = GsonBuilder().create()
    }
}
