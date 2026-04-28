package ui

import GlobalConfigs
import Publisher
import publish.JarBuilder
import util.archiveStagingFile
import util.listVersion
import util.versionCompare
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*

class BuildPanel : SimplePanel {
    private val fileBrowseButton = JButton("浏览...")
    private val folderPathField: JTextField = JTextField()
    private val versions: ConcurrentHashMap<String, JCheckBox> = ConcurrentHashMap()
    private val versionPanel: JPanel = JPanel()
    private val versionScrollPane: JScrollPane = JScrollPane(this.versionPanel)
    private val startBuildButton = JButton()
    private val currentVersion = JLabel()
    private val buttonState: AtomicReference<ButtonState> = AtomicReference(ButtonState.READY)

    constructor(registry: (JComponent) -> Unit) : super(registry) {
        this.init()
    }

    private fun init() {
        this.leftPanel.add(this.createFileChooser())
        this.leftPanel.add(Box.createVerticalStrut(10))
        this.leftPanel.add(this.createVersionCheckBox())
        this.leftPanel.add(Box.createVerticalStrut(10))
        this.leftPanel.add(this.createStartButton())
        this.leftPanel.add(Box.createVerticalGlue())
        this.leftPanel.add(this.initProgressBar())
        this.rightPanel.add(this.initCurrentVersion(), BorderLayout.SOUTH)
    }

    private fun initCurrentVersion(): JLabel {
        this.currentVersion.horizontalAlignment = SwingConstants.LEFT
        this.setCurrentVersion("无")
        return this.currentVersion
    }

    private fun setCurrentVersion(version: String) {
        val text = "当前版本：$version"
        this.invokeLaterIfAsync {
            this.currentVersion.text = text
        }
    }

    private fun createFileChooser(): JPanel {
        val folderPanel = JPanel()
        folderPanel.layout = BoxLayout(folderPanel, BoxLayout.X_AXIS)
        folderPanel.alignmentX = 0.5f
        folderPanel.maximumSize = Dimension(Integer.MAX_VALUE, 30)
        this.folderPathField.isEditable = false
        this.folderPathField.border = BorderFactory.createEtchedBorder()
        this.folderPathField.preferredSize = Dimension(0, 0)
        this.folderPathField.maximumSize = Dimension(Integer.MAX_VALUE, 30)
        this.folderPathField.text = GlobalConfigs.getRoot().absolutePath
        this.fileBrowseButton.addActionListener {
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            chooser.selectedFile = GlobalConfigs.getRoot()
            if (chooser.showOpenDialog(folderPanel) == JFileChooser.APPROVE_OPTION) {
                this.folderPathField.text = chooser.selectedFile.absolutePath
                this.refreshVersions()
            }
        }
        folderPanel.add(this.folderPathField)
        folderPanel.add(Box.createHorizontalStrut(5))
        folderPanel.add(this.fileBrowseButton)
        this.registryPanelsToHighlight(this.folderPathField)
        return folderPanel
    }

    private fun createVersionCheckBox(): JPanel {
        val panel = this.versionPanel
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        this.refreshVersions()
        val scroll = this.versionScrollPane
        scroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scroll.verticalScrollBar.unitIncrement = 16
        scroll.border = BorderFactory.createTitledBorder("选择版本")
        val wrapper = JPanel()
        wrapper.layout = BoxLayout(wrapper, BoxLayout.Y_AXIS)
        wrapper.add(scroll)
        wrapper.isFocusable = true
        this.registryPanelsToHighlight(wrapper)
        val inputMap = wrapper.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "select_all")
        val ctrlAndShift = InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrlAndShift), "deselect_all")
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "invert_selection")
        wrapper.actionMap.put("select_all", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                for (box in versions.values) {
                    box.isSelected = true
                }
            }
        })
        wrapper.actionMap.put("deselect_all", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                for (box in versions.values) {
                    box.isSelected = false
                }
            }
        })
        wrapper.actionMap.put("invert_selection", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                for (box in versions.values) {
                    box.isSelected = !box.isSelected
                }
            }
        })
        scroll.addMouseListener(this.clickToFocusInWindow(panel))
        return wrapper
    }

    private fun refreshVersions() {
        val path = Path.of(folderPathField.text)
        val versions = listVersion(path)
        this.versionPanel.removeAll()
        this.versions.clear()
        val defaultVersions = GlobalConfigs.getVersions()
        for (version in versions) {
            val box = JCheckBox(version)
            box.margin = Insets(0, 0, 0, 0)
            box.preferredSize = Dimension(box.preferredSize.width, 18)
            box.maximumSize = Dimension(box.maximumSize.width, 18)
            if (version in defaultVersions) {
                box.isSelected = true
            }
            this.versionPanel.add(box)
            this.versions[version] = box
        }
        this.versionPanel.revalidate()
        this.versionPanel.repaint()
    }

    private fun createStartButton(): JButton {
        this.setButtonState(ButtonState.READY)
        this.startBuildButton.maximumSize = Dimension(Int.MAX_VALUE, this.startBuildButton.preferredSize.height)
        this.startBuildButton.alignmentX = 0.5F
        this.startBuildButton.addActionListener {
            if (this.buttonState.get() == ButtonState.READY) {
                val list = this.versions.entries.stream()
                    .filter { it.value.isSelected }
                    .map { it.key }
                    .sorted { s1, s2 -> -versionCompare(s1, s2) }
                    .toList()
                if (list.isEmpty()) {
                    this.log("未选择任何版本！")
                    return@addActionListener
                }
                if (this.handleStaging()) {
                    return@addActionListener
                }
                this.setButtonState(ButtonState.RUNNING)
                Publisher.EXECUTOR.execute {
                    if (start(list)) {
                        return@execute
                    }
                }
            } else {
                this.setButtonState(ButtonState.WAIT_TO_STOP)
            }
        }
        return this.startBuildButton
    }

    private fun handleStaging(): Boolean {
        val stagingFiles = GlobalConfigs.getStaging().listFiles()
        if (stagingFiles != null && !stagingFiles.isEmpty()) {
            val options = arrayOf("取消", "忽略", "归档")
            val choice = JOptionPane.showOptionDialog(
                this@BuildPanel,
                "暂存区存在${stagingFiles.size}个文件。",
                "暂存区非空",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[2]
            )
            when (choice) {
                0 -> {
                    this.log("取消操作")
                    return true
                }

                1 -> {
                    this.log("已忽略暂存区文件")
                }

                2 -> {
                    stagingFiles.forEach {
                        archiveStagingFile(it)
                    }
                    this.log("已归档暂存区文件")
                }
            }
        }
        return false
    }

    private fun start(list: List<String>): Boolean {
        try {
            for ((index, version) in list.withIndex()) {
                if (this.buttonState.get() == ButtonState.WAIT_TO_STOP) {
                    return true
                }
                this.setCurrentVersion(version)
                val builder = JarBuilder(version) {
                    this.log(it)
                }
                this.log("-".repeat(80))
                try {
                    builder.run()
                } catch (e: Exception) {
                    this.log("\n构建失败！")
                    Publisher.LOGGER.error("Build failed: ", e)
                    return true
                }
                this.log("-".repeat(80))
                this.setProgress(index + 1, list.size)
            }
        } finally {
            this.setButtonState(ButtonState.READY)
            this.setCurrentVersion("无")
            this.clearLog()
        }
        return false
    }

    private fun setButtonState(state: ButtonState) {
        this.buttonState.set(state)
        this.invokeLaterIfAsync {
            this.startBuildButton.isEnabled = state != ButtonState.WAIT_TO_STOP
            this.fileBrowseButton.isEnabled = state == ButtonState.READY
        }
        when (state) {
            ButtonState.READY -> {
                this.startBuildButton.text = "开始"
            }

            ButtonState.RUNNING -> {
                this.startBuildButton.text = "停止"
            }

            ButtonState.WAIT_TO_STOP -> {
                this.startBuildButton.text = "正在停止..."
            }
        }
    }

    private enum class ButtonState {
        READY,
        RUNNING,
        WAIT_TO_STOP
    }
}
