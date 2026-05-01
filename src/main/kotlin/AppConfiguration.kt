import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class AppConfiguration {
    companion object {
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

        fun getToken(): String {
            return getJsonObject().get("token").asString
        }

        fun getDefaultSelectionVersions(): List<String> {
            val array: JsonArray = getJsonObject().get("versions").asJsonArray
            return array.asList().stream().map { it.asString }.toList()
        }

        fun getRoot(): File {
            return File(".").absoluteFile.normalize()
        }

        fun getBuildOutput(): File {
            return File(this.getRoot(), "build/libs")
        }

        fun getStaging(): File {
            return File(this.getPublisher(), "dist/staging")
        }

        fun getArchive(): File {
            return File(this.getPublisher(), "dist/archive")
        }

        fun getPublisher(): File {
            return File(this.getRoot(), "publisher")
        }

        fun getVersionSupport(): List<String> {
            val file = File(this.getPublisher(), "config/cache.json")
            if (file.isFile()) {
                val lines: List<String> = Files.readAllLines(file.toPath())
                val builder = StringBuilder()
                lines.forEach { builder.append(it) }
                val json = GSON.fromJson(builder.toString(), JsonObject::class.java)
                val timestamp = json.get("timestamp")?.asJsonPrimitive?.asLong ?: 0L
                if (System.currentTimeMillis() - timestamp < (3600L * 1000)) {
                    return json.get("versions").asJsonArray.toList().map { it.asString }.toList()
                }
            }
            val json = JsonObject()
            json.addProperty("timestamp", System.currentTimeMillis())
            val versions = this.listMinecraftVersions()
            val array = JsonArray()
            versions.forEach { array.add(it) }
            json.add("versions", array)
            Files.writeString(file.toPath(), GSON.toJson(json), StandardCharsets.UTF_8)
            return versions
        }

        private fun listMinecraftVersions(): List<String> {
            Publisher.LOGGER.info("Getting game version online")
            val url = URI.create("https://api.modrinth.com/v3/loader_field?loader_field=game_versions").toURL()
            val connection = url.openConnection()
            val input = BufferedInputStream(connection.getInputStream())
            val result: String
            input.use {
                val bytes = it.readAllBytes()
                result = String(bytes)
            }
            val array = GSON.fromJson(result, JsonArray::class.java)
            return array.asList().stream()
                .map { it.asJsonObject }
                .map { it.get("value") }
                .map { it.asString }
                .toList()
        }

        private fun getJsonObject(): JsonObject {
            val file = File(this.getPublisher(), "config/config.json")
            if (!file.exists()) {
                val json = JsonObject()
                json.add("versions", JsonArray())
                json.addProperty("token", "")
                Publisher.LOGGER.info("Init config/config.json")
                file.parentFile.mkdirs()
                file.writeText(GSON.toJson(json))
            }
            val text = file.readText()
            return GSON.fromJson(text, JsonObject::class.java)
        }
    }
}