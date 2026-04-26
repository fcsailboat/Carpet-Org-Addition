package ui

import GlobalConfigs
import Publisher
import publish.JarBuilder
import util.listVersion
import java.awt.*
import java.nio.file.Path
import java.text.DecimalFormat
import javax.swing.*
import javax.swing.border.EtchedBorder


class Screen : JFrame {
    private val folderPathField: JTextField = JTextField()
    private val progressBar: JProgressBar = JProgressBar()
    private val versions: HashMap<String, JCheckBox> = HashMap()
    private val versionPanel: JPanel = JPanel()
    private val versionScrollPane: JScrollPane = JScrollPane(this.versionPanel)

    constructor() {
        this.setSize(900, 600)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.setLocationRelativeTo(null)
        this.title = "Publisher"
        this.init()
    }

    private fun init() {
        this.initTabbedPane()
    }

    private fun initTabbedPane() {
        val tabbedPane = JTabbedPane()
        val build = this.createBuildPanel()
        val publish = JPanel(BorderLayout())
        tabbedPane.addTab("构建", build)
        tabbedPane.addTab("发布", publish)
        this.add(tabbedPane)
    }

    private fun createBuildPanel(): JPanel {
        val leftPanel = JPanel()
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.Y_AXIS)
        leftPanel.border = BorderFactory.createEmptyBorder(15, 20, 15, 20)
        leftPanel.add(this.createFileChooser())
        leftPanel.add(Box.createVerticalStrut(10))
        leftPanel.add(this.createVersionCheckBox())
        leftPanel.add(Box.createVerticalStrut(10))
        leftPanel.add(this.createStartBuildButton())
        leftPanel.add(Box.createVerticalGlue())
        leftPanel.add(this.initProgressBar())
        val rightPanel = JPanel(GridLayout(1, 0))
        val rightTextArea = JTextArea("")
        rightTextArea.border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
        rightPanel.add(rightTextArea)
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
        splitPane.resizeWeight = 0.3
        splitPane.isContinuousLayout = true
        splitPane.border = BorderFactory.createLineBorder(Color.GRAY)
        val outer = JPanel(BorderLayout())
        outer.add(splitPane, BorderLayout.CENTER)
        return outer
    }

    private fun initProgressBar(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.maximumSize = Dimension(Int.MAX_VALUE, 40)
        this.progressBar.minimum = 0
        this.progressBar.border = BorderFactory.createTitledBorder("进度条")
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

    private fun createStartBuildButton(): JButton {
        val button = JButton("开始")
        button.maximumSize = Dimension(Int.MAX_VALUE, button.preferredSize.height)
        button.alignmentX = 0.5F
        button.addActionListener {
            Publisher.EXECUTOR
            Publisher.EXECUTOR.execute {
                val list = this.versions.entries.stream()
                    .filter { it.value.isSelected }
                    .map { it.key }
                    .toList()
                for ((index, version) in list.withIndex()) {
                    val builder = JarBuilder(version)
                    builder.run()
                    this.setProgress(index + 1, list.size)
                }
            }
        }
        return button
    }

    fun display() {
        this.isVisible = true
    }
}
