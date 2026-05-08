package ui.fx

import AppConfiguration
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import publish.Metadata
import util.revealInFileManager
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView
import kotlin.jvm.optionals.getOrNull

class PublishTab : SkeletonTab() {
    private val listView = WritableUniqueListView<File>()
    private val fileIconCaches = HashMap<File, Image>()
    private val fileMetadataCaches = HashMap<File, Metadata>()
    private val stateHolder = WorkStateHolder(WorkStatus.READY)
    private val validators = ArrayList<() -> Boolean>()

    init {
        this.addFileListPanel()
        this.addPublishButton()
        this.addSpace()
        this.addProgressBar()
        Platform.runLater {
            this.setDividerPosition(0.42)
        }
    }

    private fun addPublishButton() {
        val box = HBox(8.0)
        val publishButton = Button("发布")
        val cooldownButton = CooldownButton(publishButton)
        val cancelPublishButton = Button("取消发布")
        HBox.setHgrow(publishButton, Priority.ALWAYS)
        publishButton.maxWidth = Double.MAX_VALUE
        publishButton.onAction = {
            when (this.stateHolder.workState) {
                WorkStatus.READY -> this.stateHolder.changeWorkState(WorkStatus.PENDING_CONFIRMATION)
                WorkStatus.PENDING_CONFIRMATION -> this.stateHolder.changeWorkState(WorkStatus.RUNNING)
                WorkStatus.RUNNING -> this.stateHolder.changeWorkState(WorkStatus.STOPPING)
                else -> {}
            }
        }
        this.stateHolder.addChangeValidator {
            if (it == WorkStatus.PENDING_CONFIRMATION && this.listView.isEmpty()) {
                this.logMessage("未选择任何文件！")
                false
            } else {
                true
            }
        }
        this.stateHolder.addChangeValidator {
            if (it == WorkStatus.RUNNING) {
                if (this.verify()) {
                    return@addChangeValidator true
                } else {
                    this.logMessage("取消发布！")
                    return@addChangeValidator false
                }
            }
            return@addChangeValidator true
        }
        this.stateHolder.addChangeListener {
            publishButton.isDisable = it == WorkStatus.STOPPING
            this.stateHolder.cancel = it == WorkStatus.STOPPING
        }
        this.stateHolder.addChangeListener {
            when (it) {
                WorkStatus.READY -> {
                    publishButton.text = "发布"
                }

                WorkStatus.PENDING_CONFIRMATION -> {
                    publishButton.text = "确认发布"
                }

                WorkStatus.RUNNING -> {
                    publishButton.text = "停止发布"
                }

                WorkStatus.STOPPING -> {
                    publishButton.text = "正在停止"
                }
            }
        }
        this.stateHolder.addChangeListener {
            if (it == WorkStatus.RUNNING) {
                this.publish()
            }
        }
        HBox.setHgrow(cancelPublishButton, Priority.ALWAYS)
        cancelPublishButton.onAction = {
            this.stateHolder.changeWorkState(WorkStatus.READY)
        }
        cancelPublishButton.maxWidth = Double.MAX_VALUE
        this.stateHolder.addChangeListener {
            cancelPublishButton.isVisible = it == WorkStatus.PENDING_CONFIRMATION
            cancelPublishButton.isManaged = it == WorkStatus.PENDING_CONFIRMATION
        }
        this.stateHolder.addChangeListener {
            if (it == WorkStatus.PENDING_CONFIRMATION) {
                // 防止按下发布按钮后因误触而立即确认
                cooldownButton.startCooldown()
            } else if (it == WorkStatus.READY) {
                cooldownButton.finishCooldown()
            }
        }
        this.stateHolder.changeWorkState(WorkStatus.READY)
        this.registerModFileValidator()
        cooldownButton.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(cooldownButton, Priority.ALWAYS)
        cancelPublishButton.prefWidth = 0.0
        cooldownButton.prefWidth = 0.0
        box.children.addAll(cancelPublishButton, cooldownButton)
        this.leftBox.children.add(box)
    }

    private fun registerModFileValidator() {
        this.validators.add {
            val version = this.getMetadata(this.listView[0]).version
            for (i in 1 until this.listView.size) {
                val file = this.listView[i]
                if (version != this.getMetadata(file).version) {
                    val alert = Alert(Alert.AlertType.ERROR)
                    alert.title = "拒绝发布！"
                    alert.headerText = "待发布模组版本不统一！"
                    alert.showAndWait()
                    return@add false
                }
            }
            return@add true
        }
        this.validators.add {
            val size = this.listView.size
            val versions = HashSet<List<String>>(size)
            for (i in 0 until size) {
                if (versions.add(this.getMetadata(this.listView[i]).gameVersions)) {
                    continue
                }
                val alert = Alert(Alert.AlertType.ERROR)
                alert.title = "拒绝发布！"
                alert.headerText = "待发布模组Minecraft支持版本重复！"
                alert.showAndWait()
                return@add false
            }
            return@add true
        }
        this.validators.add {
            val maxSpan = this.listView.size * 1000L * 60
            var min = this.getMetadata(this.listView[0]).timestamp
            var max = this.getMetadata(this.listView[0]).timestamp
            for (i in 1 until this.listView.size) {
                val metadata = this.getMetadata(this.listView[i])
                val timestamp = metadata.timestamp
                if (timestamp < min) {
                    min = timestamp
                } else if (timestamp > max) {
                    max = timestamp
                }
                if (max - min > maxSpan) {
                    val alert = Alert(Alert.AlertType.CONFIRMATION)
                    alert.title = "确认发布。"
                    alert.headerText = "待发布模组构建时间跨度过大，确认是否发布。"
                    val result = alert.showAndWait().getOrNull() ?: return@add false
                    return@add result == ButtonType.OK
                }
            }
            return@add true
        }
    }

    private fun getMetadata(file: File): Metadata {
        return this.fileMetadataCaches.computeIfAbsent(file) { Metadata(it) }
    }

    private fun publish() {
        val mods = this.listView.toList().stream().map { this.getMetadata(it) }.toList()
        val totals = mods.size
        val task = object : Task<Unit>() {
            override fun call() {
                updateProgress(0L, totals.toLong())
                for ((index, metadata) in mods.withIndex()) {
                    if (stateHolder.cancel) {
                        break
                    }
                    updateMessage(metadata.mcVersion)
                    Thread.sleep(3000)
                    updateProgress(index.toLong() + 1L, totals.toLong())
                }
            }
        }
        task.progressProperty().addListener { _, _, newValue ->
            this.setProgress(newValue.toDouble(), totals)
        }
        task.messageProperty().addListener { _, _, newValue ->
            this.setCurrentProceed(newValue)
        }
        task.addFinishedListener {
            when (it) {
                Worker.State.SUCCEEDED -> {
                    if (this.stateHolder.cancel) {
                        this.logMessage("发布已停止！")
                    } else {
                        this.logMessage("发布完成！")
                    }
                }

                Worker.State.FAILED -> {
                    this.logMessage("错误: ${task.exception?.message}")
                }

                else -> {}
            }
            this.stateHolder.changeWorkState(WorkStatus.READY)
        }
        Thread(task, "Publish Worker").start()
    }

    private fun verify(): Boolean {
        for (validator in this.validators) {
            if (validator()) {
                continue
            }
            return false
        }
        return true
    }

    private fun addFileListPanel() {
        val box = VBox(8.0)
        this.listView.addListChangeListener { change ->
            while (change.next()) {
                val removed = change.removed
                this.fileIconCaches.entries.removeIf { entry -> entry.key in removed }
                this.fileMetadataCaches.entries.removeIf { entry -> entry.key in removed }
            }
        }
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
        this.listView.setOnDragOver { event ->
            if (event.gestureSource != this.listView && event.dragboard.hasFiles()) {
                val files = event.dragboard.files
                if (
                    files != null
                    && files.filter { it.isFile && it.name.endsWith(".jar") }.size == files.size
                    && this.stateHolder.workState == WorkStatus.READY
                ) {
                    event.acceptTransferModes(TransferMode.COPY)
                }
            }
            event.consume()
        }
        this.listView.setOnDragDropped { event ->
            val dragboard = event.dragboard
            if (dragboard.hasFiles()) {
                this.listView.addAll(dragboard.files)
                event.setDropCompleted(true)
            } else {
                event.setDropCompleted(false)
            }
            event.consume()
        }
        val removeItem = MenuItem("移除").apply {
            this.setOnAction {
                val files = listView.selectionModel?.selectedItems?.toList() ?: listOf()
                for (file in files) {
                    listView.remove(file)
                }
            }
        }
        this.stateHolder.addChangeListener {
            removeItem.isDisable = it != WorkStatus.READY
        }
        this.listView.addContextMenu(removeItem)
        this.listView.addContextMenu(MenuItem("打开文件所在位置").apply {
            this.setOnAction {
                val file = listView.selectionModel?.selectedItem
                if (file != null) {
                    revealInFileManager(file)
                }
            }
        })
        this.listView.selectionModel?.selectionMode = SelectionMode.MULTIPLE
        box.children.add(this.listView)
        val title = TitledPane("选择文件", box)
        this.addFileListButton(box)
        this.leftBox.children.add(title)
        AppConfiguration.getStaging().listFiles()?.forEach { this.listView.add(it) }
    }

    private fun addFileListButton(parent: VBox) {
        val box = HBox(20.0)
        val clear = Button("清空")
        val selection = Button("选择...")
        val buttonWidth = 70.0
        clear.prefWidth = buttonWidth
        clear.onAction = {
            this.listView.clear()
        }
        selection.prefWidth = buttonWidth
        selection.onAction = {
            val chooser = FileChooser().apply {
                title = "选择文件"
                initialDirectory = AppConfiguration.getStaging()
            }
            val selectedFiles = chooser.showOpenMultipleDialog(selection.scene.window)
            if (selectedFiles != null) {
                this.listView.clear()
                this.listView.addAll(selectedFiles)
            }
        }
        this.stateHolder.addChangeListener {
            clear.isDisable = it != WorkStatus.READY
            selection.isDisable = it != WorkStatus.READY
        }
        box.children.add(clear)
        box.children.add(selection)
        box.alignment = Pos.CENTER
        parent.children.add(box)
    }

    private fun getFileIcon(file: File): ImageView {
        val image = this.fileIconCaches.computeIfAbsent(file) {
            val icon = FileSystemView.getFileSystemView().getSystemIcon(it, 32, 32)
            this.swingIconToJavaFXImage(icon)
        }
        return ImageView(image).apply {
            fitHeight = 16.0
            isPreserveRatio = true
            isSmooth = true
        }
    }

    private fun swingIconToJavaFXImage(icon: Icon): Image {
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = image.createGraphics()
        icon.paintIcon(null, graphics2D, 0, 0)
        graphics2D.dispose()
        return SwingFXUtils.toFXImage(image, null)
    }

    private enum class WorkStatus {
        READY,
        PENDING_CONFIRMATION,
        RUNNING,
        STOPPING
    }
}
