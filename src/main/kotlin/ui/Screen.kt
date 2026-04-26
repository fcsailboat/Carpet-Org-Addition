package ui

import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTabbedPane

class Screen : JFrame {
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
        val build = BuildPanel()
        val publish = JPanel(BorderLayout())
        tabbedPane.addTab("构建", build)
        tabbedPane.addTab("发布", publish)
        this.add(tabbedPane)
    }

    fun display() {
        this.isVisible = true
    }
}
