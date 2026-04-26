package ui

import GlobalConfigs
import Publisher
import publish.JarBuilder
import util.listVersion
import java.awt.*
import java.nio.file.Path
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.border.EtchedBorder

class BuildPanel : JPanel {
    private val folderPathField: JTextField = JTextField()
    private val progressBar: JProgressBar = JProgressBar()
    private val versions: HashMap<String, JCheckBox> = HashMap()
    private val versionPanel: JPanel = JPanel()
    private val versionScrollPane: JScrollPane = JScrollPane(this.versionPanel)
    private val button = JButton()
    private val logs: ArrayList<String> = ArrayList()
    private val rightTextArea = JTextArea()

    private var buttonState: AtomicReference<ButtonState> = AtomicReference(ButtonState.READY)

    constructor() {
        this.layout = BorderLayout()
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
        val rightPanel = JPanel(GridLayout(1, 0))
        val scroll = JScrollPane()
        scroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scroll.verticalScrollBar.unitIncrement = 16
        this.rightTextArea.border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
        scroll.setViewportView(this.rightTextArea)
        rightPanel.add(scroll)
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
        splitPane.resizeWeight = 0.3
        splitPane.isContinuousLayout = true
        splitPane.border = BorderFactory.createLineBorder(Color.GRAY)
        this.add(splitPane, BorderLayout.CENTER)
    }

    private fun initProgressBar(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.maximumSize = Dimension(Int.MAX_VALUE, 40)
        this.progressBar.minimum = 0
        this.progressBar.border = BorderFactory.createTitledBorder("进度")
        this.progressBar.isStringPainted = true
        this.setProgress(0, 0)
        panel.add(this.progressBar, BorderLayout.CENTER)
        return panel
    }

    private fun setProgress(value: Int, size: Int) {
        val text = DecimalFormat("#.##").format(100 * (value / size.toDouble()))
        SwingUtilities.invokeLater {
            progressBar.maximum = size
            progressBar.value = value
            progressBar.string = if (value == size && value == 0) "0%" else "$text% [$value/$size]"
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
        return folderPanel
    }

    private fun createVersionCheckBox(): JScrollPane {
        val panel = this.versionPanel
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        this.refreshVersions()
        val scroll = this.versionScrollPane
        scroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scroll.verticalScrollBar.unitIncrement = 16
        scroll.border = BorderFactory.createTitledBorder("选择版本")
        return scroll
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
                Publisher.EXECUTOR.execute {
                    try {
                        this.setButtonState(ButtonState.RUNNING)
                        val list = this.versions.entries.stream()
                            .filter { it.value.isSelected }
                            .map { it.key }
                            .toList()
                        for ((index, version) in list.withIndex()) {
                            if (this.buttonState.get() == ButtonState.WAIT_TO_STOP) {
                                return@execute
                            }
                            val builder = JarBuilder(version) {
                                this.log(it)
                            }
                            this.log("-".repeat(80))
                            builder.run()
                            this.log("-".repeat(80))
                            this.setProgress(index + 1, list.size)
                        }
                    } finally {
                        this.setButtonState(ButtonState.READY)
                    }
                }
            } else {
                this.setButtonState(ButtonState.WAIT_TO_STOP)
            }
        }
        return this.button
    }

    private fun log(message: String) {
        this.logs.add(message)
        val joiner = StringJoiner("\n")
        this.logs.forEach { joiner.add(it) }
        this.rightTextArea.text = joiner.toString()
    }

    private fun setButtonState(state: ButtonState) {
        this.buttonState.set(state)
        this.button.isEnabled = state != ButtonState.WAIT_TO_STOP
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
