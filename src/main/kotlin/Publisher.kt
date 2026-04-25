import build.JarBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Publisher {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("Publisher")
    }
}

fun main() {
    Publisher.LOGGER.info("Root: ${GlobalConfigs.getRoot().absoluteFile}")
    check()
    for (version in GlobalConfigs.getVersions()) {
        val builder = JarBuilder(version)
        builder.run()
    }
}

private fun check() {
    val file = GlobalConfigs.getStaging()
    if (!file.isDirectory()) {
        file.mkdirs()
    }
    val files = file.listFiles()
    if (files?.isEmpty() ?: false) {
        return
    }
    throw IllegalStateException("'$file' directory is not empty")
}
