package ui

import GlobalConfigs
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileSystemView

class PublishPanel : SimplePanel {
    private val listModel = DefaultListModel<File>()
    private val selectFiles = JList(this.listModel)
    private val fileIcons: MutableMap<File, Icon> = HashMap()

    constructor(registry: (JComponent) -> Unit) : super(registry) {
        this.init()
    }

    private fun init() {
        this.leftPanel.add(this.createFileSelection())
        this.leftPanel.add(Box.createVerticalGlue())
    }

    private fun createFileSelection(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("选择文件")
        panel.add(this.createFileSelectionList(), BorderLayout.NORTH)
        panel.add(this.createFileSelectionButton(), BorderLayout.CENTER)
        return panel
    }

    private fun createFileSelectionButton(): JPanel {
        val panel = JPanel()
        val clear = JButton("清空")
        val selection = JButton("选择...")
        panel.add(clear)
        panel.add(Box.createHorizontalStrut(5))
        panel.add(selection)
        return panel
    }

    private fun createFileSelectionList(): JPanel {
        val panel = JPanel(BorderLayout())
        val files: Array<File> = GlobalConfigs.getStaging().listFiles() ?: arrayOf()
        files.forEach { this.listModel.addElement(it) }
        this.selectFiles.visibleRowCount = Math.clamp(files.size.toLong(), 6, 15)
        this.selectFiles.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                val renderer =
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val file: File? = value as? File
                if (file != null) {
                    renderer.icon = fileIcons.computeIfAbsent(file) {
                        FileSystemView.getFileSystemView().getSystemIcon(it)
                    }
                    renderer.text = file.name
                }
                return renderer
            }
        }
        val scroll = JScrollPane(this.selectFiles)
        panel.add(scroll, BorderLayout.CENTER)
        panel.preferredSize = Dimension(0, panel.preferredSize.height)
        return panel
    }
}
