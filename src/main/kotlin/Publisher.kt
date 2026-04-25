import org.slf4j.Logger
import org.slf4j.LoggerFactory
import publish.JarUploader

class Publisher {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("Publisher")
    }
}

fun main() {
    Publisher.LOGGER.info("Root: ${GlobalConfigs.getRoot().absoluteFile}")
//    JarBuilder.start()
    JarUploader.start(GlobalConfigs.getStaging().listFiles()?.toList() ?: listOf())
}
