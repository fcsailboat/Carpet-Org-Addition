import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File

class GlobalConfigs {
    companion object {
        private val GSON: Gson = GsonBuilder().create()

        fun getToken(): String {
            return getJsonObject().get("token").asString
        }

        fun getVersions(): List<String> {
            val array: JsonArray = getJsonObject().get("versions").asJsonArray
            return array.asList().stream().map { it.asString }.toList()
        }

        fun getRoot(): File {
            return File(".").absoluteFile.normalize()
        }

        fun getBuildOutput(): File {
            return File(this.getRoot(), "publish/libs")
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

        private fun getJsonObject(): JsonObject {
            val text = File(this.getPublisher(), "config/config.json").readText()
            return GSON.fromJson(text, JsonObject::class.java)
        }
    }
}