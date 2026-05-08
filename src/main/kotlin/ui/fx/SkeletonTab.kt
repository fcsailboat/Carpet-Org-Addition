package ui.fx

import AppConfiguration
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.DirectoryChooser
import java.text.DecimalFormat

abstract class SkeletonTab : VBox() {
    protected val leftBox = VBox(5.0)
    protected val rightBox = VBox(5.0)
    private val progressBar = ProgressBar()
    private val progressBarLabel = Label()
    protected val folderPathField = TextField()
    protected val fileBrowseButton = Button("浏览...")
    private val splitPane = SplitPane()
    private val proceeding = Label()
    private val messageArea = TextArea()

    init {
        this.leftBox.padding = Insets(4.0, 3.0, 3.0, 3.0)
        this.rightBox.padding = Insets(3.0, 3.0, 3.0, 3.0)
        this.splitPane.items.addAll(this.leftBox, this.rightBox)
        this.setDividerPosition(0.35)
        setVgrow(this.splitPane, Priority.ALWAYS)
        this.children.add(this.splitPane)
        this.addMessagePanel()
    }

    private fun addMessagePanel() {
        val pane = TitledPane("信息", this.messageArea)
        this.messageArea.isEditable = false
        pane.isCollapsible = false
        pane.isAnimated = false
        pane.maxHeight = Double.MAX_VALUE
        pane.style = """-fx-border-color:lightgray;-fx-border-width:2px;-fx-border-radius:4px;"""
        this.rightBox.children.add(pane)
        setVgrow(pane, Priority.ALWAYS)
    }

    protected fun addCurrentProceed() {
        this.rightBox.children.add(this.proceeding)
    }

    protected fun addFileChooser() {
        val box = HBox(5.0)
        box.alignment = Pos.CENTER_LEFT
        box.maxWidth = Double.MAX_VALUE
        box.maxHeight = 30.0
        this.folderPathField.apply {
            isEditable = false
            style = "-fx-border-color: derive(-fx-base,-30%); -fx-border-width: 1px;"
            maxWidth = Double.MAX_VALUE
            maxHeight = 30.0
            text = AppConfiguration.getRoot().absolutePath
        }
        this.fileBrowseButton.setOnAction {
            val chooser = DirectoryChooser().apply {
                title = "选择目录"
                initialDirectory = AppConfiguration.getRoot()
            }
            val selectedDir = chooser.showDialog(this.fileBrowseButton.scene.window)
            if (selectedDir != null) {
                this.folderPathField.text = selectedDir.absolutePath
                onFileChooserUpdate()
            }
        }
        box.children.addAll(this.folderPathField, this.fileBrowseButton)
        this.folderPathField.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(this.folderPathField, Priority.ALWAYS)
        this.leftBox.children.add(box)
    }

    protected fun addProgressBar() {
        val stack = StackPane(this.progressBar, this.progressBarLabel)
        stack.alignment = Pos.CENTER
        this.progressBar.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(this.progressBar, Priority.ALWAYS)
        this.progressBar.prefHeight = 22.5
        this.leftBox.children.add(stack)
        this.setProgress(0.0, 0)
    }

    protected fun setDividerPosition(value: Double) {
        this.splitPane.setDividerPositions(*doubleArrayOf(value))
    }

    protected fun addSpace() {
        this.leftBox.children.add(Region().apply { setVgrow(this, Priority.SOMETIMES) })
    }

    protected open fun onFileChooserUpdate() {
    }

    protected fun logMessage(message: String) {
        this.messageArea.appendText("${message}\n")
        this.messageArea.end()
    }

    protected fun safetyLogMessage(message: String) {
        Platform.runLater {
            logMessage(message)
        }
    }

    protected fun clearMessage() {
        this.messageArea.clear()
    }

    protected fun setProgress(progress: Double, max: Int) {
        if (max == 0) {
            this.progressBar.progress = 0.0
            this.progressBarLabel.text = "0%"
        } else {
            this.progressBar.progress = progress
            this.progressBarLabel.text = "${FORMATTER.format(100 * progress)}% [${(progress * max).toInt()}/$max]"
        }
    }

    protected fun Task<Unit>.addFinishedListener(listener: (Worker.State) -> Unit) {
        this.stateProperty().addListener { _, _, newValue ->
            when (newValue) {
                Worker.State.SUCCEEDED, Worker.State.CANCELLED, Worker.State.FAILED -> {
                    listener(newValue)
                }

                else -> {}
            }
        }
    }

    protected fun setCurrentProceed(left: String, right: String) {
        this.proceeding.text = "$left: $right"
    }

    protected fun setCurrentProceed(right: String) {
        this.setCurrentProceed("当前版本", right)
    }

    companion object {
        private val FORMATTER = DecimalFormat("#.##")
        const val CELL_SIZE = 20.0
    }
}
