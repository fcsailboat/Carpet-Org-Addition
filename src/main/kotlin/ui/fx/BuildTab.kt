package ui.fx

import AppConfiguration
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.TitledPane
import javafx.scene.control.cell.CheckBoxListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import org.apache.commons.collections4.list.SetUniqueList
import util.listVersion
import java.nio.file.Path


class BuildTab : SimpleTab() {
    private val observableList = FXCollections.observableList(SetUniqueList.setUniqueList(ArrayList<String>()))
    private val listView = ListView(this.observableList)
    private val checkStates = HashMap<String, BooleanProperty>()
    private val startBuildButton = Button()
    private var workState = WorkStatus.READY

    init {
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
            checkStates.getOrPut(it) {
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
        this.observableList.clear()
        this.observableList.addAll(versions)
        this.listView.prefHeight = CELL_SIZE * versions.size
    }

    private fun addStartButton() {
        val box = HBox()
        box.children.add(this.startBuildButton)
        HBox.setHgrow(this.startBuildButton, Priority.ALWAYS)
        this.startBuildButton.maxWidth = Double.MAX_VALUE
        this.setButtonState(WorkStatus.READY)
        this.leftBox.children.add(box)
    }

    private fun setButtonState(state: WorkStatus) {
        this.workState = state
        this.startBuildButton.isDisable = state == WorkStatus.WAIT_TO_STOP
        this.fileBrowseButton.isDisable = state != WorkStatus.READY

        when (state) {
            WorkStatus.READY -> {
                this.startBuildButton.text = "开始构建"
            }

            WorkStatus.RUNNING -> {
                this.startBuildButton.text = "停止构建"
            }

            WorkStatus.WAIT_TO_STOP -> {
                this.startBuildButton.text = "正在停止..."
            }
        }
    }

    private enum class WorkStatus {
        READY,
        RUNNING,
        WAIT_TO_STOP
    }
}