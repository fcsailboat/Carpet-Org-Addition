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
    private val fileSelectionPanel = JPanel(BorderLayout())

    constructor(registry: (JComponent) -> Unit) : super(registry) {
        this.init()
    }

    private fun init() {
        this.leftPanel.preferredSize = Dimension(250, this.leftPanel.preferredSize.height)
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
        clear.addActionListener {
            this.listModel.clear()
            this.updateFileListVisibleRows()
        }
        val selection = JButton("选择...")
        selection.addActionListener {
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
            chooser.currentDirectory = GlobalConfigs.getStaging()
            chooser.isMultiSelectionEnabled = true
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                val files = chooser.selectedFiles.toList()
                this.listModel.clear()
                this.listModel.addAll(files)
                this.updateFileListVisibleRows()
            }
        }
        panel.add(clear)
        panel.add(Box.createHorizontalStrut(5))
        panel.add(selection)
        return panel
    }

    private fun createFileSelectionList(): JPanel {
        val files: Array<File> = GlobalConfigs.getStaging().listFiles() ?: arrayOf()
        files.forEach { this.listModel.addElement(it) }
        this.updateFileListVisibleRows()
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
        this.fileSelectionPanel.add(scroll, BorderLayout.CENTER)
        return this.fileSelectionPanel
    }

    private fun updateFileListVisibleRows() {
        this.invokeLaterIfAsync {
            this.selectFiles.visibleRowCount = Math.clamp(this.listModel.size.toLong(), 6, 15)
            this.fileSelectionPanel.revalidate()
            this.fileSelectionPanel.repaint()
        }
    }
}
