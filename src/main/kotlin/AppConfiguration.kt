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
import kotlin.io.path.absolutePathString

class AppConfiguration {
    companion object {
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
        private lateinit var configJson: JsonObject

        fun getToken(): String {
            return readConfig().get("token").asString
        }

        fun getDefaultSelectionVersions(): List<String> {
            val array: JsonArray = readConfig().get("selection_versions").asJsonArray
            return array.asList().stream().map { it.asString }.toList()
        }

        private fun getConfigDirectory(): File {
            return File("./publisher", "config")
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
            val map = this.readConfig().get("java_paths")
                ?.asJsonObject?.entrySet()
                ?.associate { (key, value) -> key to Path.of(value.asString) }
                ?: mapOf()
            return map["java_$version"]
                ?: throw IllegalArgumentException("Missing or invalid 'java_paths' for version $version")
        }

        fun getJavaDependVersion(minecraftVersion: String): Int {
            val entries = this.readConfig().get("java_depends")?.asJsonObject?.entrySet() ?: setOf()
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

        private fun readConfig(): JsonObject {
            if (this::configJson.isInitialized) {
                return this.configJson
            }
            val file = File(this.getConfigDirectory(), "config.json")
            if (!file.exists()) {
                val json = initConfig()
                file.parentFile.mkdirs()
                file.writeText(GSON.toJson(json))
            }
            val text = file.readText()
            this.configJson = GSON.fromJson(text, JsonObject::class.java)
            return this.configJson
        }

        private fun initConfig(): JsonObject {
            val json = JsonObject()
            json.add("selection_versions", JsonArray())
            json.addProperty("token", "*".repeat(64))
            json.addProperty("working_directory", Path.of(".").absolutePathString())
            val javaPaths = JsonObject()
            javaPaths.addProperty("java_${Runtime.version().feature()}", System.getProperty("java.home"))
            json.add("java_paths", javaPaths)
            val javaDepends = JsonObject()
            javaDepends.addProperty("1.20.4-", 17)
            javaDepends.addProperty("1.20.5-1.21.11", 21)
            javaDepends.addProperty("26.1+", 25)
            json.add("java_depends", javaDepends)
            Publisher.LOGGER.info("Init config/config.json")
            return json
        }
    }
}
