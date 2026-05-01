package ui

import util.listVersion
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Path
import javax.swing.*
import kotlin.math.max

class MergeBranchPanel : SimplePanel {
    private val listModel = NonRepetitiveListModel<String>()
    private val selectVersions = JList(this.listModel)

    constructor(registry: (JComponent) -> Unit) : super(registry) {
        this.init()
    }

    private fun init() {
        this.leftPanel.add(this.createFileChooser())
        this.leftPanel.add(Box.createVerticalStrut(10))
        this.leftPanel.add(this.createSortableList())
        this.leftPanel.add(Box.createVerticalGlue())
        this.leftPanel.add(this.initProgressBar())
    }

    override fun onFileChooserUpdate() {
        this.refreshBranches()
    }

    private fun refreshBranches() {
        val path = Path.of(this.folderPathField.text)
        val versions = listVersion(path).reversed()
        this.listModel.clear()
        this.listModel.addAll(versions)
        this.selectVersions.visibleRowCount = max(this.listModel.size, 6)
    }

    private fun createSortableList(): JPanel {
        this.refreshBranches()
        this.selectVersions.selectionMode = ListSelectionModel.SINGLE_SELECTION
        this.selectVersions.dragEnabled = true
        this.selectVersions.transferHandler = ListItemTransferHandler()
        this.selectVersions.dropMode = DropMode.INSERT
        val scroll = JScrollPane(selectVersions)
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("选择分支")
        panel.add(scroll, BorderLayout.CENTER)
        panel.maximumSize = Dimension(Int.MAX_VALUE, scroll.preferredSize.height)
        return panel
    }
}