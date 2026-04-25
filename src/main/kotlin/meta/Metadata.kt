package meta

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

    constructor(file: File) {
        this.file = file
        val zip = ZipFile(file)
        val entry: ZipEntry = zip.getEntry("fabric.mod.json") ?: throw IllegalArgumentException()
        val input = zip.getInputStream(entry)
        input.use {
            val json = GSON.fromJson(String(it.readAllBytes()), JsonObject::class.java)
            this.version = json.get("version").asString
            this.id = json.get("id").asString
            this.mcVersion = json.getAsJsonObject("depends").get("minecraft").asString
        }
    }

    fun getFileName(): String {
        return this.file.name
    }

    fun getVersionType(): VersionType {
        if (this.mcVersion.contains("snapshot")) {
            return VersionType.SNAPSHOT
        }
        if (this.mcVersion.contains("pre")) {
            return VersionType.PRE_RELEASE
        }
        if (this.mcVersion.contains("rc")) {
            return VersionType.RELEASE_CANDIDATE
        }
        if (this.mcVersion.matches(Regex("(\\d+\\.\\d+)|(\\d+\\.\\d+\\.\\d+)"))) {
            return VersionType.OFFICIAL
        }
        throw IllegalStateException("Unknown version type")
    }

    companion object {
        private val GSON: Gson = GsonBuilder().create()
    }
}

enum class VersionType {
    OFFICIAL,
    SNAPSHOT,
    PRE_RELEASE,
    RELEASE_CANDIDATE
}
