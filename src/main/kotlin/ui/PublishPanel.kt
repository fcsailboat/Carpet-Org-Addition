package ui

import AppConfiguration
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileSystemView
import kotlin.math.max

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
        this.leftPanel.add(Box.createVerticalStrut(10))
        this.leftPanel.add(this.createPublishButton())
        this.leftPanel.add(Box.createVerticalGlue())
        this.leftPanel.add(this.initProgressBar())
    }

    private fun createPublishButton(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        val button = JButton("发布")
        panel.add(button, BorderLayout.CENTER)
        panel.maximumSize = Dimension(Int.MAX_VALUE, button.preferredSize.height)
        button.alignmentX = 0.5F
        return panel
    }

    private fun createFileSelection(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("选择文件")
        panel.add(this.createFileSelectionList(), BorderLayout.CENTER)
        panel.add(this.createFileSelectionButton(), BorderLayout.SOUTH)
        panel.maximumSize = Dimension(Int.MAX_VALUE, panel.preferredSize.height)
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
            chooser.currentDirectory = AppConfiguration.getStaging()
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
        val files: Array<File> = AppConfiguration.getStaging().listFiles() ?: arrayOf()
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
        this.setupFilePopupMenu()
        val scroll = JScrollPane(this.selectFiles)
        scroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
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

    private fun setupFilePopupMenu() {
        val popup = JPopupMenu()
        val removeItem = JMenuItem("移除")
        val openFile = JMenuItem("打开文件所在位置")
        removeItem.addActionListener {
            // 倒序移除，避免下标变化导致错误
            val indices = this.selectFiles.selectedIndices.sortedDescending()
            for (i in indices) {
                this.listModel.remove(i)
            }
            this.updateFileListVisibleRows()
        }
        openFile.addActionListener {
            val index = this.selectFiles.selectedIndex
            val file: File = this.listModel.get(index)
            Runtime.getRuntime().exec(arrayOf("explorer", "/select,", file.absolutePath))
        }
        popup.add(removeItem)
        if (System.getProperty("os.name").lowercase().contains("win")) {
            popup.add(openFile)
        }
        this.selectFiles.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    this.showPopup(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    this.showPopup(e)
                }
            }

            private fun showPopup(e: MouseEvent) {
                val index = selectFiles.locationToIndex(e.point)
                if (index != -1) {
                    if (!selectFiles.isSelectedIndex(index)) {
                        selectFiles.selectedIndex = index
                    }
                    popup.show(selectFiles, e.x, e.y)
                }
            }
        })
    }

    private fun updateFileListVisibleRows() {
        this.selectFiles.visibleRowCount = max(this.listModel.size + 1, 6)
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
