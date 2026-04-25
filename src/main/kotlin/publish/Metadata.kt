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
        this.subtitle = "${this.id}-mc${this.mcVersion}-${this.version}"
    }

    companion object {
        private val GSON: Gson = GsonBuilder().create()
    }
}