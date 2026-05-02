package ui

import AppConfiguration
import util.listVersion
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.*
import kotlin.math.max

class MergeBranchPanel : SimplePanel {
    private val listModel = NonRepetitiveListModel<String>()
    private val versionChecks = HashSet<String>()
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
        this.prioritizeCheckedVersions(versions)
        this.selectVersions.visibleRowCount = max(this.listModel.size, 6)
    }

    private fun createSortableList(): JPanel {
        AppConfiguration.getDefaultSelectionVersions().forEach { this.versionChecks.add(it) }
        this.refreshBranches()
        this.selectVersions.selectionMode = ListSelectionModel.SINGLE_SELECTION
        this.selectVersions.dragEnabled = true
        this.selectVersions.transferHandler = ListItemTransferHandler { this.versionChecks.size }
        this.selectVersions.dropMode = DropMode.INSERT
        this.selectVersions.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                val index = selectVersions.locationToIndex(e.point)
                if (index < 0) {
                    return
                }
                val cellBounds = selectVersions.getCellBounds(index, index) ?: return
                val checkBoxWidth = JCheckBox().preferredSize.width
                val checkBoxArea = Rectangle(cellBounds.x, cellBounds.y, checkBoxWidth, cellBounds.height)
                if (checkBoxArea.contains(e.point)) {
                    val branch = listModel.getElementAt(index)
                    if (branch.isChecked()) {
                        versionChecks.remove(branch)
                    } else {
                        versionChecks.add(branch)
                    }
                    prioritizeCheckedVersions()
                    selectVersions.repaint(cellBounds)
                }
            }
        })
        this.selectVersions.cellRenderer = object : ListCellRenderer<String> {
            private val checkBox = JCheckBox()

            override fun getListCellRendererComponent(
                list: JList<out String>,
                value: String,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                checkBox.isSelected = value.isChecked()
                checkBox.text = if (checkBox.isSelected) value else "<html><s>$value</s></html>"
                checkBox.background = if (isSelected) list.selectionBackground else list.background
                checkBox.foreground = if (isSelected) list.selectionForeground else list.foreground
                checkBox.isOpaque = true
                return checkBox
            }
        }
        val scroll = JScrollPane(selectVersions)
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("选择分支")
        panel.add(scroll, BorderLayout.CENTER)
        panel.maximumSize = Dimension(Int.MAX_VALUE, scroll.preferredSize.height)
        return panel
    }

    private fun String.isChecked(): Boolean {
        return this in versionChecks
    }

    private fun prioritizeCheckedVersions(branches: Iterable<String> = this.listModel) {
        val checked = mutableListOf<String>()
        val unchecked = mutableListOf<String>()
        for (branch in branches) {
            if (branch.isChecked()) {
                checked.add(branch)
            } else {
                unchecked.add(branch)
            }
        }
        this.listModel.clear()
        this.listModel.addAll(checked)
        this.listModel.addAll(unchecked)
    }
}
