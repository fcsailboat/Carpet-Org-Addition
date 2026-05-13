package ui.fx

import Publisher
import javafx.beans.property.BooleanProperty
import javafx.scene.control.TitledPane
import org.eclipse.jgit.api.Git
import publish.Branch
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
        val title = TitledPane("选择分支", this.listView)
        this.listView.cellFactory = {
            SortableListCell(this.checkStates) { it.name }
        }
        this.leftBox.children.add(title)
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
