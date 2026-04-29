import com.formdev.flatlaf.FlatLightLaf
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ui.Screen
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class Publisher {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("Publisher")
        val EXECUTOR: Executor = Executors.newSingleThreadExecutor()
    }
}

fun main() {
    Publisher.LOGGER.info("Root: ${AppConfiguration.getRoot().absoluteFile}")
//    JarUploader.start(GlobalConfigs.getStaging().listFiles()?.toList() ?: listOf())
    FlatLightLaf.setup()
    val screen = Screen()
    screen.display()
}
