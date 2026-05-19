import javafx.application.Application
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ui.MainScreen

class Publisher {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("Publisher")
    }
}

fun main() {
    Publisher.LOGGER.info("Root directory: ${AppConfiguration.getRoot().absoluteFile}")
    Application.launch(MainScreen::class.java)
}
