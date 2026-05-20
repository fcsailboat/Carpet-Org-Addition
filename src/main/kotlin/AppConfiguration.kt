import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import publish.MinecraftVersion
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class AppConfiguration {
    companion object {
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
        private lateinit var configJson: JsonObject

        fun getToken(): String {
            return getJsonObject().get("token").asString
        }

        fun getDefaultSelectionVersions(): List<String> {
            val array: JsonArray = getJsonObject().get("selection_versions").asJsonArray
            return array.asList().stream().map { it.asString }.toList()
        }

        private fun getConfigDirectory(): File {
            return File("./publisher", "config")
        }

        private fun readConfig(): JsonObject {
            val file = File(this.getConfigDirectory(), "config.json")
            val str = Files.readString(file.absoluteFile.toPath(), Charsets.UTF_8)
            return GSON.fromJson(str, JsonObject::class.java)
        }

        fun getRoot(): File {
            return File(".").absoluteFile.normalize()
        }

        fun getDefaultWorkingDirectory(): File {
            val json = this.readConfig()
            val path = json.getAsJsonPrimitive("working_directory")?.asString ?: "."
            return Path.of(path).toFile().absoluteFile.normalize()
        }

        fun getBuildOutput(workingDirectory: File): File {
            return File(workingDirectory, "build/libs")
        }

        fun getStaging(): File {
            return File(this.getPublisher(), "dist/staging")
        }

        fun getArchive(): File {
            return File(this.getPublisher(), "dist/archive")
        }

        private fun getPublisher(): File {
            return File(this.getRoot(), "publisher")
        }

        fun getJavaPath(version: Int): Path {
            val map = this.getJsonObject().get("java_paths")
                ?.asJsonObject?.entrySet()
                ?.associate { (key, value) -> key to Path.of(value.asString) }
                ?: mapOf()
            return map["java_$version"]
                ?: throw IllegalArgumentException("Missing or invalid 'java_paths' for version $version")
        }

        fun getJavaDependVersion(minecraftVersion: String): Int {
            val entries = this.getJsonObject().get("java_depends")?.asJsonObject?.entrySet() ?: setOf()
            for (entry in entries) {
                val predicate = this.parseJavaDependVersion(entry.key)
                if (predicate(minecraftVersion)) {
                    return entry.value.asJsonPrimitive.asInt
                }
            }
            throw IllegalArgumentException(minecraftVersion)
        }

        private fun parseJavaDependVersion(expression: String): (String) -> Boolean {
            if (MinecraftVersion(expression).isValid()) {
                return { it == expression }
            }
            val split = expression.split("-").filter { it.isNotEmpty() }
            if (split.size == 1 && expression.endsWith("+")) {
                return { MinecraftVersion(it) >= MinecraftVersion(split[0].removeSuffix("+")) }
            }
            if (split.size == 1 && expression.endsWith("-")) {
                return { MinecraftVersion(it) <= MinecraftVersion(split[0]) }
            }
            if (split.size == 2) {
                return {
                    MinecraftVersion(it) >= MinecraftVersion(split[0]) && MinecraftVersion(it) <= MinecraftVersion(split[1])
                }
            }
            throw IllegalArgumentException("Unable to parse Java dependency versions: $expression")
        }

        fun getVersionSupport(): List<String> {
            val file = File(this.getConfigDirectory(), "cache.json")
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
            if (this::configJson.isInitialized) {
                return this.configJson
            }
            val file = File(this.getConfigDirectory(), "config.json")
            if (!file.exists()) {
                val json = JsonObject()
                json.add("versions", JsonArray())
                json.addProperty("token", "")
                Publisher.LOGGER.info("Init config/config.json")
                file.parentFile.mkdirs()
                file.writeText(GSON.toJson(json))
            }
            val text = file.readText()
            this.configJson = GSON.fromJson(text, JsonObject::class.java)
            return this.configJson
        }
    }
}
