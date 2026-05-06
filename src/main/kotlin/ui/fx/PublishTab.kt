package ui.fx

import AppConfiguration
import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TitledPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import org.apache.commons.collections4.list.SetUniqueList
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

class PublishTab : SimpleTab() {
    private val observableList = FXCollections.observableList(SetUniqueList.setUniqueList(ArrayList<File>()))
    private val publishFiles = ListView(this.observableList)
    private val fileIconCaches = HashMap<File, ImageView>()

    init {
        this.addFileListPanel()
    }

    private fun addFileListPanel() {
        val box = VBox()
        AppConfiguration.getStaging().listFiles()?.forEach { this.observableList.add(it) }
        box.children.add(this.publishFiles)
        this.publishFiles.setCellFactory {
            object : ListCell<File>() {
                override fun updateItem(file: File?, empty: Boolean) {
                    super.updateItem(file, empty)
                    if (empty || file == null) {
                        text = null
                        graphic = null
                    } else {
                        text = file.name
                        graphic = getFileIcon(file)
                    }
                }
            }
        }
        val title = TitledPane("选择文件", box)
        this.leftBox.children.add(title)
    }

    private fun getFileIcon(file: File): ImageView {
        return this.fileIconCaches.computeIfAbsent(file) {
            val icon = FileSystemView.getFileSystemView().getSystemIcon(it, 32, 32)
            return@computeIfAbsent ImageView(this.swingIconToJavaFXImage(icon)).apply {
                fitHeight = 16.0
                isPreserveRatio = true
                isSmooth = true
            }
        }
    }

    private fun swingIconToJavaFXImage(icon: Icon): Image {
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = image.createGraphics()
        icon.paintIcon(null, graphics2D, 0, 0)
        graphics2D.dispose()
        return SwingFXUtils.toFXImage(image, null)
    }
}