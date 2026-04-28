package ui

import GlobalConfigs
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.*
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
            this.clearFiles()
        }
        val selection = JButton("选择...")
        selection.addActionListener {
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
            chooser.currentDirectory = GlobalConfigs.getStaging()
            chooser.isMultiSelectionEnabled = true
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                val files = chooser.selectedFiles.toList()
                this.clearFiles()
                this.addFiles(files)
            }
        }
        panel.add(clear)
        panel.add(Box.createHorizontalStrut(5))
        panel.add(selection)
        return panel
    }

    private fun createFileSelectionList(): JPanel {
        val files: Array<File> = GlobalConfigs.getStaging().listFiles() ?: arrayOf()
        this.addFiles(files.toList())
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
        this.setupFileDrop()
        val scroll = JScrollPane(this.selectFiles)
        this.fileSelectionPanel.add(scroll, BorderLayout.CENTER)
        return this.fileSelectionPanel
    }

    private fun setupFileDrop() {
        this.selectFiles.transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
            }

            override fun importData(support: TransferSupport): Boolean {
                if (this.canImport(support)) {
                    val transferable = support.transferable
                    val fileList = try {
                        val data = transferable.getTransferData(DataFlavor.javaFileListFlavor)
                        data as? List<*> ?: return false
                    } catch (_: Exception) {
                        return false
                    }
                    val files = fileList.filterIsInstance<File>()
                        .filter { it.isFile }
                        .filter { !listModel.contains(it) }
                    if (files.isEmpty()) return false
                    addFiles(files)
                    return true
                }
                return false
            }
        }
        this.selectFiles.dropMode = DropMode.USE_SELECTION
        this.selectFiles.dragEnabled = false
    }

    private fun updateFileListVisibleRows() {
        this.selectFiles.visibleRowCount = Math.clamp(this.listModel.size.toLong() + 1, 6, 15)
        this.fileSelectionPanel.revalidate()
        this.fileSelectionPanel.repaint()
    }

    private fun addFiles(files: List<File>) {
        val tree = TreeSet<File>()
        tree.addAll(files)
        this.invokeLaterIfAsync {
            for (i in 0 until this.listModel.size) {
                tree.add(this.listModel[i])
            }
            this.listModel.clear()
            this.listModel.addAll(tree)
            this.updateFileListVisibleRows()
        }
    }

    private fun clearFiles() {
        this.invokeLaterIfAsync {
            this.listModel.clear()
            this.updateFileListVisibleRows()
        }
    }
}
