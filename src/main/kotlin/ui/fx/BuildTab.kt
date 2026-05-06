package ui.fx

import AppConfiguration
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.CheckBoxListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import org.apache.commons.collections4.list.SetUniqueList
import publish.JarBuilder
import util.archiveStagingFile
import util.listVersion
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max


class BuildTab : SimpleTab() {
    private val versionList = FXCollections.observableList(SetUniqueList.setUniqueList(ArrayList<String>()))
    private val listView = ListView(this.versionList)
    private val checkStates = HashMap<String, BooleanProperty>()
    private val startBuildButton = Button()
    private var workState = WorkStatus.READY
    private val stopFlag = AtomicBoolean(false)

    init {
        this.addCurrentProceed()
        this.addFileChooser()
        this.addVersionList()
        this.addStartButton()
        this.addSpace()
        this.addProgressBar()
    }

    private fun addVersionList() {
        val titledPane = TitledPane("选择版本", this.listView)
        this.listView.fixedCellSize = CELL_SIZE
        this.listView.cellFactory = CheckBoxListCell.forListView {
            this.checkStates.getOrPut(it) {
                SimpleBooleanProperty(false)
            }
        }
        this.leftBox.children.add(titledPane)
        this.refreshVersionList()
    }

    override fun onFileChooserUpdate() {
        this.refreshVersionList()
    }

    private fun refreshVersionList() {
        val versions = listVersion(Path.of(this.folderPathField.text))
        this.checkStates.clear()
        for (version in AppConfiguration.getDefaultSelectionVersions()) {
            this.checkStates[version] = SimpleBooleanProperty(true)
        }
        this.versionList.clear()
        this.versionList.addAll(versions)
        this.listView.prefHeight = CELL_SIZE * max(versions.size, 3)
    }

    private fun addStartButton() {
        val box = HBox()
        box.children.add(this.startBuildButton)
        HBox.setHgrow(this.startBuildButton, Priority.ALWAYS)
        this.startBuildButton.maxWidth = Double.MAX_VALUE
        this.setButtonState(WorkStatus.READY)
        this.startBuildButton.setOnAction {
            when (this.workState) {
                WorkStatus.RUNNING -> this.setButtonState(WorkStatus.WAIT_TO_STOP)
                WorkStatus.READY -> this.startBuildTask()
                else -> {}
            }
        }
        this.leftBox.children.add(box)
    }

    private fun startBuildTask() {
        val list = this.versionList.stream().filter { it.isChecked() }.toList().reversed()
        if (list.isEmpty()) {
            this.logMessage("未选择任何版本！")
            return
        }
        if (this.handleStaging()) {
            this.onTaskStarted()
            val totals = list.size
            val task = object : Task<Unit>() {
                override fun call() {
                    updateProgress(0L, totals.toLong())
                    for ((index, version) in list.withIndex()) {
                        if (stopFlag.get()) {
                            break
                        }
                        updateMessage(version)
                        val builder = JarBuilder(version) { safetyLogMessage(it) }
                        safetyLogMessage("-".repeat(70))
                        builder.run()
                        safetyLogMessage("-".repeat(70))
                        updateProgress(index.toLong() + 1, totals.toLong())
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
                        if (this.stopFlag.get()) {
                            this.logMessage("构建已停止！")
                        } else {
                            this.logMessage("构建完成！")
                        }
                    }

                    Worker.State.FAILED -> {
                        this.logMessage("错误: ${task.exception?.message}")
                    }

                    else -> {}
                }
                this.setButtonState(WorkStatus.READY)
            }
            Thread(task, "BuildWorker").start()
        }
    }

    private fun onTaskStarted() {
        this.setButtonState(WorkStatus.RUNNING)
        this.stopFlag.set(false)
    }

    private fun handleStaging(): Boolean {
        val files = AppConfiguration.getStaging().listFiles()
        if (files == null || files.isEmpty()) {
            return true
        }
        val alert = Alert(Alert.AlertType.WARNING).apply {
            this.dialogPane = object : DialogPane() {
                override fun createButtonBar(): Node {
                    return super.createButtonBar().apply {
                        if (this is ButtonBar) {
                            this.buttonOrder = ButtonBar.BUTTON_ORDER_NONE
                        }
                    }
                }
            }
        }
        alert.title = "暂存区非空"
        alert.headerText = "暂存区存在${files.size}个文件等待处理。"
        alert.buttonTypes.setAll(CANCEL_BUTTON, IGNORE_BUTTON, ARCHIVE_BUTTON)
        (alert.dialogPane.lookupButton(ARCHIVE_BUTTON) as? Button)?.isDefaultButton = true
        (alert.dialogPane.lookupButton(CANCEL_BUTTON) as? Button)?.isCancelButton = true
        Platform.runLater {
            alert.dialogPane.lookupButton(ARCHIVE_BUTTON)?.requestFocus()
        }
        val result: ButtonType? = alert.showAndWait().orElse(null)
        return when (result) {

            IGNORE_BUTTON -> {
                this.logMessage("已忽略暂存区文件。")
                true
            }

            ARCHIVE_BUTTON -> {
                files.forEach { archiveStagingFile(it) }
                this.logMessage("已归档暂存区文件。")
                true
            }

            else -> {
                this.logMessage("取消操作！")
                false
            }
        }
    }

    private fun String.isChecked(): Boolean {
        return checkStates[this]?.value ?: false
    }

    private fun setButtonState(state: WorkStatus) {
        this.workState = state
        this.fileBrowseButton.isDisable = state != WorkStatus.READY
        this.startBuildButton.isDisable = state == WorkStatus.WAIT_TO_STOP
        this.listView.isDisable = state != WorkStatus.READY
        when (state) {
            WorkStatus.READY -> {
                this.startBuildButton.text = "开始构建"
                this.setCurrentProceed("无")
            }

            WorkStatus.RUNNING -> {
                this.startBuildButton.text = "停止构建"
            }

            WorkStatus.WAIT_TO_STOP -> {
                this.startBuildButton.text = "正在停止..."
                this.stopFlag.set(true)
            }
        }
    }

    private enum class WorkStatus {
        READY,
        RUNNING,
        WAIT_TO_STOP
    }

    private companion object {
        private val CANCEL_BUTTON = ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE)
        private val IGNORE_BUTTON = ButtonType("忽略", ButtonBar.ButtonData.OTHER)
        private val ARCHIVE_BUTTON = ButtonType("归档", ButtonBar.ButtonData.OK_DONE)
    }
}