package ui

import GlobalConfigs
import Publisher
import publish.JarBuilder
import util.listVersion
import util.versionCompare
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.*
import java.nio.file.Path
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*

class BuildPanel : JPanel {
    private val folderPathField: JTextField = JTextField()
    private val progressBar: JProgressBar = JProgressBar()
    private val versions: HashMap<String, JCheckBox> = HashMap()
    private val versionPanel: JPanel = JPanel()
    private val versionScrollPane: JScrollPane = JScrollPane(this.versionPanel)
    private val button = JButton()
    private val logs: ArrayList<String> = ArrayList()
    private val rightTextArea = JTextArea()
    private val currentVersion = JLabel()
    private val buttonState: AtomicReference<ButtonState> = AtomicReference(ButtonState.READY)
    private val registryPanelsToHighlight: (JComponent) -> Unit

    constructor(registry: (JComponent) -> Unit) {
        this.layout = BorderLayout()
        this.registryPanelsToHighlight = registry
        this.init()
    }

    private fun init() {
        val leftPanel = JPanel()
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.Y_AXIS)
        leftPanel.border = BorderFactory.createEmptyBorder(15, 20, 15, 20)
        leftPanel.add(this.createFileChooser())
        leftPanel.add(Box.createVerticalStrut(10))
        leftPanel.add(this.createVersionCheckBox())
        leftPanel.add(Box.createVerticalStrut(10))
        leftPanel.add(this.createStartButton())
        leftPanel.add(Box.createVerticalGlue())
        leftPanel.add(this.initProgressBar())
        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(this.createScrollTextArea(), BorderLayout.CENTER)
        rightPanel.add(this.initCurrentVersion(), BorderLayout.SOUTH)
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
        splitPane.resizeWeight = 0.3
        splitPane.isContinuousLayout = true
        splitPane.border = BorderFactory.createLineBorder(Color.GRAY)
        this.add(splitPane, BorderLayout.CENTER)
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

    private fun invokeLaterIfAsync(run: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            run()
        } else {
            SwingUtilities.invokeLater { run() }
        }
    }

    private fun createScrollTextArea(): JScrollPane {
        val scroll = JScrollPane()
        scroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scroll.verticalScrollBar.unitIncrement = 16
        this.rightTextArea.border = BorderFactory.createTitledBorder("日志")
        this.rightTextArea.isEditable = false
        scroll.setViewportView(this.rightTextArea)
        this.registryPanelsToHighlight(scroll)
        return scroll
    }

    private fun initProgressBar(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.maximumSize = Dimension(Int.MAX_VALUE, 40)
        this.progressBar.minimum = 0
        this.progressBar.border = BorderFactory.createTitledBorder("进度")
        this.progressBar.isStringPainted = true
        this.setProgress(0, 0)
        panel.add(this.progressBar, BorderLayout.CENTER)
        panel.isFocusable = true
        this.registryPanelsToHighlight(panel)
        panel.addMouseListener(this.clickToFocusInWindow(panel))
        return panel
    }

    private fun setProgress(value: Int, size: Int) {
        val text = DecimalFormat("#.##").format(100 * (value / size.toDouble()))
        this.invokeLaterIfAsync {
            this.progressBar.maximum = size
            this.progressBar.value = value
            this.progressBar.string = if (size == 0) "0%" else "$text% [$value/$size]"
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
        val browseButton = JButton("浏览...")
        browseButton.addActionListener {
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
        folderPanel.add(browseButton)
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
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "deselect_all")
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

    private fun clickToFocusInWindow(panel: JPanel): MouseAdapter = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            panel.requestFocusInWindow()
        }
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
        this.button.maximumSize = Dimension(Int.MAX_VALUE, button.preferredSize.height)
        this.button.alignmentX = 0.5F
        this.button.addActionListener {
            if (this.buttonState.get() == ButtonState.READY) {
                if ((GlobalConfigs.getStaging().listFiles()?.size ?: 0) > 0) {

                }
                val list = this.versions.entries.stream()
                    .filter { it.value.isSelected }
                    .map { it.key }
                    .sorted { s1, s2 -> -versionCompare(s1, s2) }
                    .toList()
                if (list.isEmpty()) {
                    this.log("未选择任何版本！")
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
        return this.button
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
            this.invokeLaterIfAsync {
                this.logs.clear()
            }
        }
        return false
    }

    private fun log(message: String) {
        this.invokeLaterIfAsync {
            this.logs.add(message)
            val joiner = StringJoiner("\n")
            this.logs.forEach { joiner.add(it) }
            this.rightTextArea.text = joiner.toString()
        }
    }

    private fun setButtonState(state: ButtonState) {
        this.buttonState.set(state)
        this.invokeLaterIfAsync {
            this.button.isEnabled = state != ButtonState.WAIT_TO_STOP
        }
        when (state) {
            ButtonState.READY -> {
                this.button.text = "开始"
            }

            ButtonState.RUNNING -> {
                this.button.text = "停止"
            }

            ButtonState.WAIT_TO_STOP -> {
                this.button.text = "正在停止..."
            }
        }
    }

    private enum class ButtonState {
        READY,
        RUNNING,
        WAIT_TO_STOP
    }
}
