package ui

import AppConfiguration
import Publisher
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.eclipse.jgit.api.Git
import publish.Branch
import publish.MinecraftVersion
import util.startChildProcess
import java.io.File
import java.io.IOException
import kotlin.jvm.optionals.getOrNull

class MergeBranchTab : SkeletonTab() {
    private val listView = WritableUniqueListView<Branch>()
    private val checkStates = HashMap<Branch, BooleanProperty>()
    private val branches = HashMap<String, Branch>()
    private val stateHolder = WorkStateHolder<WorkStatus>(WorkStatus.READY)
    private val runClient = CheckBox("运行客户端")

    init {
        this.addCurrentProceed()
        this.addFileChooser()
        this.addBranchList()
        this.addMergeButton()
        this.addRunClientCheckBox()
        this.addSpace()
        this.addProgressBar()
    }

    private fun addRunClientCheckBox() {
        val box = HBox()
        this.runClient.isSelected = true
        box.children.add(this.runClient)
        this.leftBox.children.add(box)
    }

    private fun addMergeButton() {
        val box = HBox()
        val button = Button()
        this.stateHolder.addChangeValidator {
            if (it == WorkStatus.RUNNING) {
                if (this.isOrdered()) {
                    return@addChangeValidator true
                }
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "分支未排序"
                alert.headerText = "已选择的分支尚未按版本排序，是否继续合并？"
                val result = alert.showAndWait().getOrNull() ?: return@addChangeValidator false
                return@addChangeValidator result == ButtonType.OK
            }
            return@addChangeValidator true
        }
        this.stateHolder.addChangeListener {
            when (it) {
                WorkStatus.READY -> button.text = "合并分支"
                WorkStatus.RUNNING -> button.text = "停止合并"
                WorkStatus.STOPPING -> button.text = "正在停止"
            }
        }
        this.stateHolder.addChangeValidator {
            if (it == WorkStatus.RUNNING && this.listView.toList().count { branch -> branch.isChecked() } < 2) {
                this.logMessage("至少需要选择两个版本！")
                return@addChangeValidator false
            }
            return@addChangeValidator true
        }
        this.stateHolder.addChangeListener {
            if (it == WorkStatus.RUNNING) {
                this.mergeBranch()
            }
        }
        this.stateHolder.addChangeListener {
            button.isDisable = it == WorkStatus.STOPPING
            this.stateHolder.cancel = it == WorkStatus.STOPPING
        }
        this.stateHolder.addChangeListener {
            this.fileBrowseButton.isDisable = it != WorkStatus.READY
            this.runClient.isDisable = it != WorkStatus.READY
        }
        this.stateHolder.addChangeListener {
            if (it == WorkStatus.READY) {
                this.setCurrentProceed("当前状态", "合并未开始")
            }
        }
        button.onAction = {
            when (this.stateHolder.workState) {
                WorkStatus.READY -> this.stateHolder.changeWorkState(WorkStatus.RUNNING)
                WorkStatus.RUNNING -> this.stateHolder.changeWorkState(WorkStatus.STOPPING)
                else -> {}
            }
        }
        button.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(button, Priority.ALWAYS)
        box.children.add(button)
        this.stateHolder.changeWorkState(WorkStatus.READY)
        this.leftBox.children.add(box)
    }

    private fun mergeBranch() {
        this.clearMessage()
        val branches = this.getCheckedBranches()
        val totals = branches.size
        val task = object : Task<Unit>() {
            override fun call() {
                updateProgress(0L, totals.toLong())
                for (index in 1 until totals) {
                    if (stateHolder.cancel) {
                        break
                    }
                    val prev = branches[index - 1]
                    val current = branches[index]
                    updateMessage("${current.name} <- ${prev.name}")
                    logEmptyMessage()
                    logDividingLineLater()
                    logMessageLater("正在将${prev.name}合并到${current.name}")
                    val skipped = current.acceptMerge(prev) {
                        logMessageLater(it)
                    }
                    if (runClient.isSelected) {
                        if (skipped) {
                            logMessageLater("已跳过启动客户端")
                        } else {
                            logMessageLater("正在启动客户端")
                            val command = listOf("gradlew", "runClient")
                            val directory = File(folderPathField.text)
                            val javaVersion = AppConfiguration.getJavaDependVersion(current.name)
                            val javaPath = AppConfiguration.getJavaPath(javaVersion)
                            val process = startChildProcess(command, directory, javaPath)
                            val exitCode = process.waitFor()
                            if (exitCode != 0) {
                                throw IllegalStateException("客户端异常终止，退出码：$exitCode")
                            }
                            logMessageLater("完成")
                        }
                    }
                    updateProgress(index.toLong(), totals.toLong() - 1)
                }
            }
        }
        task.progressProperty().addListener { _, _, newValue ->
            this.setProgress(newValue.toDouble(), totals - 1)
        }
        task.messageProperty().addListener { _, _, newValue ->
            this.setCurrentProceed("当前状态", newValue)
        }
        task.addFinishedListener {
            try {
                when (it) {
                    Worker.State.SUCCEEDED -> {
                        if (this.stateHolder.cancel) {
                            this.logMessage("合并已被取消！")
                        } else {
                            this.logMessage("合并完成！")
                        }
                    }

                    Worker.State.FAILED -> {
                        val e = task.exception
                        this.logMessage("错误: ${e?.asString()}")
                        this.logMessage("合并异常终止！")
                        Publisher.LOGGER.error("Cannot merge: ", e)
                    }

                    else -> {}
                }
            } finally {
                this.stateHolder.changeWorkState(WorkStatus.READY)
            }
        }
        Thread(task, "Merge Branch Worker").start()
    }

    private fun isOrdered(): Boolean {
        val list = this.getCheckedBranches()
        for (i in 0 until list.size - 1) {
            val current = list[i]
            val next = list[i + 1]
            if (MinecraftVersion(current.name) >= MinecraftVersion(next.name)) {
                return false
            }
        }
        return true
    }

    private fun getCheckedBranches(): List<Branch> =
        this.listView.toList().stream().filter { branch -> branch.isChecked() }.toList()

    private fun addBranchList() {
        val box = VBox(3.0)
        this.listView.sorter = { o1, o2 ->
            if (o1.isChecked() && !o2.isChecked()) {
                -1
            } else if (!o1.isChecked() && o2.isChecked()) {
                1
            } else {
                0
            }
        }
        this.listView.cellFactory = {
            SortableListCell(this.checkStates, { it.name }, {
                this.checkStates.entries.stream().filter { it.value.value }.count().toInt() - 1
            }).apply {
                this.checkBoxChangeListener = {
                    this@MergeBranchTab.listView.sort()
                }
                this.freeze(this@MergeBranchTab.stateHolder.workState != WorkStatus.READY)
                this@MergeBranchTab.stateHolder.addChangeListener {
                    this.freeze(it != WorkStatus.READY)
                }
            }
        }
        val button = Button("自动排序")
        button.onAction = { this.listView.sort { o1, o2 -> comparator(o1, o2) } }
        this.stateHolder.addChangeListener {
            val disable = it != WorkStatus.READY
            button.isDisable = disable
        }
        button.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(button, Priority.ALWAYS)
        box.style = "-fx-padding: 1px;"
        box.children.add(this.listView)
        box.children.add(button)
        val title = TitledPane("选择分支", box)
        this.leftBox.children.add(title)
    }

    private fun comparator(o1: Branch, o2: Branch): Int {
        if (o1.isChecked() && !o2.isChecked()) {
            return -1
        }
        if (!o1.isChecked() && o2.isChecked()) {
            return 1
        }
        if (o1.isChecked() && o2.isChecked()) {
            return MinecraftVersion(o1.name).compareTo(MinecraftVersion(o2.name))
        }
        return 0
    }

    private fun Branch.isChecked(): Boolean {
        return checkStates[this]?.get() ?: false
    }

    override fun onFileChooserUpdate() {
        this.branches.clear()
        try {
            val git = Git.open(File(this.folderPathField.text))
            this.listView.addAll(Branch.listLocalBranch(git))
            for (branch in this.listView) {
                this.branches[branch.name] = branch
            }
        } catch (e: IOException) {
            this.listView.clear()
            Publisher.LOGGER.error("无法打开Git仓库：${this.folderPathField.text}", e)
        }
        for (branch in this.listView) {
            if (branch.name in AppConfiguration.getDefaultSelectionVersions()) {
                val property = this.checkStates.getOrPut(branch) {
                    SimpleBooleanProperty(true)
                }
                property.value = true
            }
        }
        this.listView.sort { o1, o2 ->
            comparator(o1, o2)
        }
    }

    private enum class WorkStatus {
        READY,
        RUNNING,
        STOPPING
    }
}
