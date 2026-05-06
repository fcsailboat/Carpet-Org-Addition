package ui.fx

import AppConfiguration
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TitledPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import org.apache.commons.collections4.list.SetUniqueList
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView
import kotlin.math.max

class PublishTab : SimpleTab() {
    private val fileList = FXCollections.observableList(SetUniqueList.setUniqueList(ArrayList<File>()))
    private val listView = ListView(this.fileList)
    private val fileIconCaches = HashMap<File, ImageView>()

    init {
        this.addFileListPanel()
        this.addSpace()
        this.addProgressBar()
        Platform.runLater {
            this.setDividerPosition(0.42)
        }
    }

    private fun addFileListPanel() {
        val box = VBox(8.0)
        this.fileList.addListener(ListChangeListener { change ->
            while (change.next()) {
                val removed = change.removed
                this.fileIconCaches.entries.removeIf { entry -> entry.key in removed }
            }
            this.listView.prefHeight = CELL_SIZE * max(this.fileList.size, 2) + 24
        })
        this.listView.fixedCellSize = CELL_SIZE
        this.listView.cellFactory = {
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
        box.children.add(this.listView)
        val title = TitledPane("选择文件", box)
        this.addFileListButton(box)
        this.leftBox.children.add(title)
        AppConfiguration.getStaging().listFiles()?.forEach { this.fileList.add(it) }
    }

    private fun addFileListButton(parent: VBox) {
        val box = HBox(20.0)
        val clear = Button("清空")
        val selection = Button("选择...")
        val buttonWidth = 70.0
        clear.prefWidth = buttonWidth
        clear.onAction = {
            this.fileList.clear()
        }
        selection.prefWidth = buttonWidth
        selection.onAction = {
            val chooser = FileChooser().apply {
                title = "选择文件"
                initialDirectory = AppConfiguration.getStaging()
            }
            val selectedFiles = chooser.showOpenMultipleDialog(selection.scene.window)
            if (selectedFiles != null) {
                this.fileList.clear()
                this.fileList.addAll(selectedFiles)
            }
        }
        box.children.add(clear)
        box.children.add(selection)
        box.alignment = Pos.CENTER
        parent.children.add(box)
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