package ui.fx

import Publisher
import javafx.beans.property.BooleanProperty
import javafx.scene.control.Button
import javafx.scene.control.TitledPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.eclipse.jgit.api.Git
import publish.Branch
import util.versionCompare
import java.io.File
import java.io.IOException

class MergeBranchTab : SkeletonTab() {
    private val listView = WritableUniqueListView<Branch>()
    private val checkStates = HashMap<Branch, BooleanProperty>()
    private val branches = HashMap<String, Branch>()

    init {
        this.addCurrentProceed()
        this.addFileChooser()
        this.addBranchList()
    }

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
            }
        }
        val button = Button("自动排序")
        button.onAction = {
            this.listView.sort { o1, o2 ->
                if (o1.isChecked() && !o2.isChecked()) {
                    -1
                } else if (!o1.isChecked() && o2.isChecked()) {
                    1
                } else if (o1.isChecked() && o2.isChecked()) {
                    -versionCompare(o1.name, o2.name)
                } else {
                    0
                }
            }
        }
        button.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(button, Priority.ALWAYS)
        box.style = "-fx-padding: 1px;"
        box.children.add(this.listView)
        box.children.add(button)
        val title = TitledPane("选择分支", box)

        this.leftBox.children.add(title)
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
    }
}
