package ui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.FileSystemException
import java.text.DecimalFormat
import java.util.*
import javax.swing.*

open class SimplePanel : JPanel {
    protected val registryPanelsToHighlight: (JComponent) -> Unit
    protected val leftPanel = JPanel()
    protected val rightPanel = JPanel(BorderLayout())
    protected val rightTextArea = JTextArea()
    private val logs: ArrayList<String> = ArrayList()
    private val progressBar: JProgressBar = JProgressBar()

    constructor(registry: (JComponent) -> Unit) {
        this.registryPanelsToHighlight = registry
        this.init()
    }

    private fun init() {
        this.layout = BorderLayout()
        this.initSplitPane()
        this.leftPanel.layout = BoxLayout(this.leftPanel, BoxLayout.Y_AXIS)
        this.leftPanel.border = BorderFactory.createEmptyBorder(15, 20, 15, 20)
        this.rightPanel.add(this.createScrollTextArea(), BorderLayout.CENTER)
    }

    private fun initSplitPane() {
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftPanel, this.rightPanel)
        splitPane.resizeWeight = 0.3
        splitPane.isContinuousLayout = true
        splitPane.border = BorderFactory.createLineBorder(Color.GRAY)
        this.add(splitPane, BorderLayout.CENTER)
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

    protected fun invokeLaterIfAsync(run: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            run()
        } else {
            SwingUtilities.invokeLater { run() }
        }
    }

    protected fun log(message: String = "") {
        this.invokeLaterIfAsync {
            this.logs.add(message)
            val joiner = StringJoiner("\n")
            this.logs.forEach { joiner.add(it) }
            this.rightTextArea.text = joiner.toString()
        }
    }

    protected fun clearLog() {
        this.invokeLaterIfAsync {
            this.logs.clear()
        }
    }

    protected fun initProgressBar(): JPanel {
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

    protected fun setProgress(value: Int, size: Int) {
        val text = DecimalFormat("#.##").format(100 * (value / size.toDouble()))
        this.invokeLaterIfAsync {
            progressBar.maximum = size
            progressBar.value = value
            progressBar.string = if (size == 0) "0%" else "$text% [$value/$size]"
        }
    }

    protected fun fileOperationFailed(operation: () -> Unit): Boolean {
        try {
            operation()
            return false
        } catch (e: FileSystemException) {
            this.invokeLaterIfAsync {
                this.fileErrorPopUpWindow(e)
            }
            return true
        }
    }

    private fun fileErrorPopUpWindow(error: FileSystemException) {
        JOptionPane.showMessageDialog(
            this,
            error.message ?: error.javaClass.simpleName,
            "文件系统异常",
            JOptionPane.ERROR_MESSAGE
        )
    }

    protected fun clickToFocusInWindow(panel: JPanel): MouseAdapter = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            panel.requestFocusInWindow()
        }
    }
}