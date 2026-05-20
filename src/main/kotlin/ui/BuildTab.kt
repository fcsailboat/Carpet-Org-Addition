package ui

import AppConfiguration
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.CheckBoxListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.util.Callback
import org.eclipse.jgit.api.Git
import publish.JarBuilder
import util.archiveStagingFile
import util.listVersion
import java.io.File
import java.nio.file.Path.of


class BuildTab : SkeletonTab() {
    private val listView = WritableUniqueListView<String>()
    private val checkStates = HashMap<String, BooleanProperty>()
    private val stateHolder = WorkStateHolder(WorkStatus.READY)
    private val skipTests = CheckBox("跳过单元测试")

    init {
        this.addCurrentProceed()
        this.addFileChooser()
        this.addVersionList()
        this.addStartButton()
        this.addSkipTestsCheckBox()
        this.addSpace()
        this.addProgressBar()
    }

    private fun addSkipTestsCheckBox() {
        val box = HBox()
        box.children.add(this.skipTests)
        this.stateHolder.addChangeListener {
            this.skipTests.isDisable = it != WorkStatus.READY
        }
        this.leftBox.children.add(box)
    }

    private fun addVersionList() {
        val titledPane = TitledPane("选择版本", this.listView)
        this.listView.cellFactory = Callback<ListView<String>, ListCell<String>> {
            object : CheckBoxListCell<String>({ item ->
                checkStates.getOrPut(item) { SimpleBooleanProperty(false) }
            }) {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    tooltip = if (!empty && item != null) {
                        Tooltip("Java版本：${AppConfiguration.getJavaDependVersion(item)}")
                    } else {
                        null
                    }
                }
            }.apply {
                this.isDisable = stateHolder.workState != WorkStatus.READY
                stateHolder.addChangeListener {
                    this.isDisable = it != WorkStatus.READY
                }
            }
        }
        this.leftBox.children.add(titledPane)
    }

    override fun onFileChooserUpdate() {
        val versions = listVersion(of(folderPathField.text))
        checkStates.clear()
        for (version in AppConfiguration.getDefaultSelectionVersions()) {
            checkStates[version] = SimpleBooleanProperty(true)
        }
        this.listView.clear()
        this.listView.addAll(versions)
    }

    private fun addStartButton() {
        val box = HBox()
        val button = Button()
        box.children.add(button)
        HBox.setHgrow(button, Priority.ALWAYS)
        button.maxWidth = Double.MAX_VALUE
        this.stateHolder.addChangeListener {
            this.fileBrowseButton.isDisable = it != WorkStatus.READY
            button.isDisable = it == WorkStatus.STOPPING
        }
        this.stateHolder.addChangeListener {
            if (it == WorkStatus.READY) {
                this.setCurrentProceed("无")
            }
        }
        this.stateHolder.addChangeListener {
            if (it == WorkStatus.STOPPING) {
                this.stateHolder.cancel = true
            }
        }
        this.stateHolder.addChangeListener {
            when (it) {
                WorkStatus.READY -> {
                    button.text = "开始构建"
                }

                WorkStatus.RUNNING -> {
                    button.text = "停止构建"
                }

                WorkStatus.STOPPING -> {
                    button.text = "正在停止..."
                }
            }
        }
        this.stateHolder.changeWorkState(WorkStatus.READY)
        button.setOnAction {
            when (this.stateHolder.workState) {
                WorkStatus.RUNNING -> this.stateHolder.changeWorkState(WorkStatus.STOPPING)
                WorkStatus.READY -> this.startBuildTask()
                else -> {}
            }
        }
        this.leftBox.children.add(box)
    }

    private fun startBuildTask() {
        val list = this.listView.toList().stream().filter { it.isChecked() }.toList().reversed()
        if (list.isEmpty()) {
            this.logMessage("未选择任何版本！")
            return
        }
        if (this.handleStaging()) {
            this.stateHolder.changeWorkState(WorkStatus.RUNNING)
            this.stateHolder.cancel = false
            val totals = list.size
            val task = object : Task<Unit>() {
                override fun call() {
                    updateProgress(0L, totals.toLong())
                    val workingDirectory = File(folderPathField.text)
                    val git = Git.open(workingDirectory)
                    for ((index, version) in list.withIndex()) {
                        if (stateHolder.cancel) {
                            break
                        }
                        updateMessage(version)
                        logEmptyMessage()
                        logDividingLineLater()
                        val builder = JarBuilder(
                            git,
                            workingDirectory,
                            version,
                            skipTests.isSelected
                        ) { logMessageLater(it) }
                        builder.run()
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
                try {
                    when (it) {
                        Worker.State.SUCCEEDED -> {
                            if (this.stateHolder.cancel) {
                                this.logMessage("构建已停止！")
                            } else {
                                this.logMessage("构建完成！")
                            }
                        }

                        Worker.State.FAILED -> {
                            this.logMessage("错误: ${task.exception?.asString()}")
                        }

                        else -> {}
                    }
                } finally {
                    this.stateHolder.changeWorkState(WorkStatus.READY)
                }
            }
            Thread(task, "Build Worker").start()
        }
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

    private enum class WorkStatus {
        READY,
        RUNNING,
        STOPPING
    }

    private companion object {
        private val CANCEL_BUTTON = ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE)
        private val IGNORE_BUTTON = ButtonType("忽略", ButtonBar.ButtonData.OTHER)
        private val ARCHIVE_BUTTON = ButtonType("归档", ButtonBar.ButtonData.OK_DONE)
    }
}