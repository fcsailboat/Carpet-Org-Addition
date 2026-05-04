import javafx.application.Application
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ui.fx.MainScreen
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class Publisher {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("Publisher")
        val EXECUTOR: Executor = Executors.newSingleThreadExecutor {
            val thread = Thread(it)
            thread.name = "Worker Thread"
            thread
        }
    }
}

fun main() {
    Publisher.LOGGER.info("Working directory: ${AppConfiguration.getRoot().absoluteFile}")
    Application.launch(MainScreen::class.java)
//    Screen().display()
}
