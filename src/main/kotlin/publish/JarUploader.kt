package publish

import AppConfiguration
import Publisher
import com.google.gson.Gson
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class JarUploader {
    private val file: File
    private val metadata: Metadata
    private val logger: (String) -> Unit

    constructor(metadata: Metadata, logger: (String) -> Unit) {
        this.file = metadata.file
        this.metadata = metadata
        this.logger = logger
    }

    fun upload(listed: Boolean): Int {
        if (this.metadata.gameVersions.isEmpty()) {
            throw ModMetadataException("${this.metadata.subtitle} is not applicable to any Minecraft version")
        }
        val code = this.request(listed)
        this.logger("完成发布")
        return code
    }

    private fun request(listed: Boolean): Int {
        this.logger("正在创建请求")
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
        body["status"] = if (listed) "listed" else "unlisted"
        val json: String = GSON.toJson(body)
        val httpClient = HttpClients.createDefault()
        httpClient.use { httpClient ->
            val post = HttpPost(URL)
            post.setHeader("Authorization", AppConfiguration.getToken())
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
            this.logger("正在发布")
            val response = httpClient.execute(post) {
                val entity = it.entity
                val message = if (entity == null) null else EntityUtils.toString(entity)
                if (it.code != EFFECTIVE_RESPONSE && entity != null) {
                    Publisher.LOGGER.error(message)
                }
                return@execute Pair(it.code, message)
            }
            if (response.first != EFFECTIVE_RESPONSE) {
                throw ModrinthPublishException(response.first, response.second)
            }
            return response.first
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
        val versions: List<String> = AppConfiguration.getVersionSupport()
    }
}
