package publish

import GlobalConfigs
import Publisher
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.io.entity.EntityUtils
import util.moveOrReplaceFile
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*

class JarUploader {
    private val file: File
    private val metadata: Metadata

    constructor(file: File) {
        this.file = file
        this.metadata = Metadata(file)
    }

    private fun upload(): Int {
        if (this.metadata.gameVersions.isEmpty()) {
            throw ModPublishException("${this.metadata.subtitle} is not applicable to any Minecraft version")
        }
        return this.request()
    }

    private fun request(): Int {
        val body: HashMap<String, Any> = HashMap()
        body["name"] = this.metadata.subtitle
        body["version_number"] = this.metadata.version
        body["version_type"] = "release"
        body["dependencies"] = this.depend()
        body["game_versions"] = this.metadata.gameVersions
        body["loaders"] = Collections.singletonList("fabric")
        body["project_id"] = PROJECT
        body["file_parts"] = Collections.singletonList(this.file.name)
        body["primary_file"] = this.file.name
        body["featured"] = true
        val json: String = GSON.toJson(body)
        val httpClient = HttpClients.createDefault()
        httpClient.use { httpClient ->
            val post = HttpPost(URL)
            post.setHeader("Authorization", GlobalConfigs.getToken())
            post.setHeader("User-Agent", "https://github.com/fcsailboat/Carpet-Org-Addition")
            val builder = MultipartEntityBuilder.create()
            builder.addBinaryBody(
                "data",
                json.toByteArray(StandardCharsets.UTF_8),
                ContentType.APPLICATION_JSON,
                "data.json"
            )
            builder.addBinaryBody(
                this.file.name,
                this.file,
                ContentType.APPLICATION_OCTET_STREAM,
                this.file.name
            )
            val entity: HttpEntity = builder.build()
            post.entity = entity
            return httpClient.execute(post) {
                val entity = it.entity
                if (it.code != EFFECTIVE_RESPONSE && entity != null) {
                    Publisher.LOGGER.error(EntityUtils.toString(entity))
                }
                return@execute it.code
            }
        }
    }

    private fun depend(): List<Map<String, Any>> {
        val list = ArrayList<Map<String, Any>>()
        list.add(mapOf(Pair("project_id", CARPET), Pair("dependency_type", "required")))
        list.add(mapOf(Pair("project_id", FABRIC_API), Pair("dependency_type", "required")))
        return list
    }

    companion object {
        private const val CARPET = "TQTTVgYE"
        private const val FABRIC_API = "P7dR8mSH"
        private const val PROJECT = "L0bOPIqR"
        private const val URL = "https://api.modrinth.com/v2/version"
        private const val EFFECTIVE_RESPONSE = 200
        private val GSON: Gson = Gson()
        val versions: List<String> = this.listMinecraftVersions()

        fun start(files: List<File>) {
            Publisher.LOGGER.info("Check the files to be published: ")
            Publisher.LOGGER.info("-".repeat(70))
            files.stream()
                .map { Metadata(it) }
                .map { "${it.subtitle} ${it.gameVersions}" }
                .forEach { Publisher.LOGGER.info(it) }
            Publisher.LOGGER.info("-".repeat(70))
            Publisher.LOGGER.info("Confirm to publish these ${files.size} mod(s) to Modrinth? Y/N")
            val input: String = readln()
            if (input == "y" || input == "Y") {
                for ((index, file) in files.withIndex()) {
                    Publisher.LOGGER.info("[${index}/${files.size}] Publishing ${file.name} to Modrinth...")
                    val uploader = JarUploader(file)
                    val code = uploader.upload()
                    if (code == EFFECTIVE_RESPONSE) {
                        Publisher.LOGGER.info("Published, status code: $code")
                        moveOrReplaceFile(file.toPath(), File(GlobalConfigs.getArchive(), file.name).toPath())
                    } else {
                        Publisher.LOGGER.error("Publish failed, status code: $code")
                        throw IllegalStateException("${file.name} failed to publish to Modrinth, status code: $code")
                    }
                }
            } else {
                Publisher.LOGGER.info("Abort publishing!")
                return
            }
        }

        private fun listMinecraftVersions(): List<String> {
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
    }
}
