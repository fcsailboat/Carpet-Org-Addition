package ui

import AppConfiguration
import Publisher
import publish.Metadata
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.*
import javax.swing.filechooser.FileSystemView
import kotlin.math.max

class PublishPanel : SimplePanel {
    private val listModel = DefaultListModel<File>()
    private val selectFiles = JList(this.listModel)
    private val metadataCache = ConcurrentHashMap<File, Metadata>()
    private val fileIcons: MutableMap<File, Icon> = HashMap()
    private val fileSelectionPanel = JPanel(BorderLayout())
    private val publishButton = JButton()
    private val cancelButton = JButton("取消")

    @Volatile
    private var buttonState: ButtonState = ButtonState.READY
    private var lastClickReadyTime: Long = 0L

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
        this.rightPanel.add(this.initCurrentVersion(), BorderLayout.SOUTH)
    }

    private fun createPublishButton(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        this.setButtonState(ButtonState.READY)
        this.publishButton.addActionListener {
            if (this.buttonState == ButtonState.READY) {
                if (this.listModel.isEmpty) {
                    this.log("请选择要发布的文件！")
                    return@addActionListener
                }
                this.setButtonState(ButtonState.PENDING_CONFIRM)
                this.lastClickReadyTime = System.currentTimeMillis()
                this.clearLog()
                val size = this.listModel.size
                for (i in 0 until size) {
                    val file = this.listModel[i]
                    val metadata = this.getMetadata(file)
                    this.log("[${metadata.subtitle}]  /  ${metadata.gameVersions}")
                }
                this.log()
                this.log("确认将以上${size}个模组发布到Modrinth？")
                return@addActionListener
            }
            if (this.buttonState == ButtonState.RUNNING) {
                this.setButtonState(ButtonState.WAIT_TO_STOP)
                return@addActionListener
            }
            if (this.buttonState == ButtonState.PENDING_CONFIRM) {
                // 阻止按下发布按钮后立即确认（防误触）
                if (System.currentTimeMillis() - this.lastClickReadyTime < 1000) {
                    this.log("操作过于频繁！")
                    return@addActionListener
                }
                val reason = this.checkModFileError()
                if (reason != RefusalPublishReason.NONE && reason.dialog(this@PublishPanel)) {
                    return@addActionListener
                }
                this.setButtonState(ButtonState.RUNNING)
                val files = ArrayList<File>(this.listModel.size)
                for (i in 0 until this.listModel.size) {
                    files.add(this.listModel[i])
                }
                this.log("开始发布，共${files.size}个文件")
                Publisher.EXECUTOR.execute {
                    try {
                        for ((index, file) in files.withIndex()) {
                            val failed = this.fileOperationFailed {
                                if (this.buttonState == ButtonState.WAIT_TO_STOP) {
                                    return@fileOperationFailed
                                }
                                val metadata = getMetadata(file)
                                this.setCurrentVersion(metadata.mcVersion)
                                // TODO 替换为实际发布功能
                                Thread.sleep(2000)
                                this.setProgress(index + 1, files.size)
                            }
                            if (failed) {
                                this.log("发布中止！")
                                break
                            }
                        }
                    } finally {
                        this.setButtonState(ButtonState.READY)
                        this.setCurrentVersion(null)
                    }
                }
            }
        }
        this.cancelButton.addActionListener {
            this.setButtonState(ButtonState.READY)
            this.log("取消发布！")
        }
        panel.add(this.publishButton, BorderLayout.CENTER)
        panel.add(this.cancelButton, BorderLayout.SOUTH)
        panel.maximumSize = Dimension(Int.MAX_VALUE, this.publishButton.preferredSize.height)
        this.publishButton.alignmentX = 0.5F
        return panel
    }

    private fun checkModFileError(): RefusalPublishReason {
        if (this.checkModVersionInconsistent()) {
            return RefusalPublishReason.VERSION_INCONSISTENT
        }
        if (this.checkMinecraftVersionRepeat()) {
            return RefusalPublishReason.VERSION_REPEAT
        }
        if (this.checkModBuildTimespan()) {
            return RefusalPublishReason.LARGE_TIME_SPAN
        }
        return RefusalPublishReason.NONE
    }

    private fun checkModVersionInconsistent(): Boolean {
        val version = this.getMetadata(this.listModel[0]).version
        for (i in 1 until this.listModel.size) {
            val file = this.listModel[i]
            if (version != this.getMetadata(file).version) {
                return true
            }
        }
        return false
    }

    private fun checkMinecraftVersionRepeat(): Boolean {
        val size = this.listModel.size
        val versions = HashSet<List<String>>(size)
        for (i in 0 until size) {
            if (versions.add(this.getMetadata(this.listModel[i]).gameVersions)) {
                continue
            }
            return true
        }
        return false
    }

    private fun checkModBuildTimespan(): Boolean {
        val maxSpan = this.listModel.size * 1000L * 60
        var min = this.getMetadata(this.listModel[0]).timestamp
        var max = this.getMetadata(this.listModel[0]).timestamp
        for (i in 1 until this.listModel.size) {
            val metadata = this.getMetadata(this.listModel[i])
            val timestamp = metadata.timestamp
            if (timestamp < min) {
                min = timestamp
            } else if (timestamp > max) {
                max = timestamp
            }
            if (max - min > maxSpan) {
                return true
            }
        }
        return false
    }

    private fun getMetadata(file: File): Metadata {
        return this.metadataCache.computeIfAbsent(file) { Metadata(it) }
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
            if (this.buttonState != ButtonState.READY) {
                this.fileListImmutableDialog()
                return@addActionListener
            }
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
        this.selectFiles.addListSelectionListener {
            if (this.buttonState != ButtonState.READY) {
                return@addListSelectionListener
            }
            this.clearLog()
            for (file in this.selectFiles.selectedValuesList) {
                val metadata = Metadata(file)
                this.log("标题：${metadata.subtitle}")
                this.log("模组版本：${metadata.version}")
                this.log("游戏版本：${metadata.gameVersions}")
                this.log("构建时间：${metadata.getFormatTime()}")
                this.log()
            }
        }
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
        val removeItem = JMenuItem("移除选中文件")
        val openFile = JMenuItem("打开文件所在位置")
        removeItem.addActionListener {
            if (this.buttonState != ButtonState.READY) {
                this.fileListImmutableDialog()
                return@addActionListener
            }
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
            if (this.buttonState != ButtonState.READY) {
                this.fileListImmutableDialog()
                return@invokeLaterIfAsync
            }
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
            if (this.buttonState != ButtonState.READY) {
                this.fileListImmutableDialog()
                return@invokeLaterIfAsync
            }
            this.listModel.clear()
            this.updateFileListVisibleRows()
            this.metadataCache.clear()
        }
    }

    private fun fileListImmutableDialog() {
        JOptionPane.showMessageDialog(
            this,
            "发布操作未完成或取消，不可变更文件",
            "文件列表已锁定",
            JOptionPane.WARNING_MESSAGE
        )
    }

    private fun setButtonState(state: ButtonState) {
        this.invokeLaterIfAsync {
            this.buttonState = state
            this.publishButton.isEnabled = state != ButtonState.WAIT_TO_STOP
            this.cancelButton.isVisible = state == ButtonState.PENDING_CONFIRM
            when (state) {
                ButtonState.READY -> {
                    this.publishButton.text = "发布"
                    this.setProgress(0, 0)
                }

                ButtonState.PENDING_CONFIRM -> {
                    this.publishButton.text = "确认发布"
                }

                ButtonState.RUNNING -> {
                    this.publishButton.text = "停止发布"
                }

                ButtonState.WAIT_TO_STOP -> {
                    this.publishButton.text = "正在停止"
                }
            }
        }
    }

    private enum class ButtonState {
        READY,
        PENDING_CONFIRM,
        RUNNING,
        WAIT_TO_STOP
    }

    private enum class RefusalPublishReason {
        VERSION_INCONSISTENT {
            override fun dialog(parentComponent: Component): Boolean {
                JOptionPane.showMessageDialog(
                    parentComponent,
                    "待发布模组版本不统一",
                    "拒绝发布",
                    JOptionPane.ERROR_MESSAGE
                )
                return true
            }
        },
        VERSION_REPEAT {
            override fun dialog(parentComponent: Component): Boolean {
                JOptionPane.showMessageDialog(
                    parentComponent,
                    "待发布模组Minecraft支持版本重复",
                    "拒绝发布",
                    JOptionPane.ERROR_MESSAGE
                )
                return true
            }
        },
        LARGE_TIME_SPAN {
            override fun dialog(parentComponent: Component): Boolean {
                val choice = JOptionPane.showConfirmDialog(
                    parentComponent,
                    "待发布模组构建时间跨度过大，确认是否发布",
                    "确认发布",
                    JOptionPane.WARNING_MESSAGE
                )
                return choice != 0
            }
        },
        NONE;

        open fun dialog(parentComponent: Component): Boolean {
            return false
        }
    }
}
