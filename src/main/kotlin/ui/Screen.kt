package ui

import java.awt.Color
import java.awt.Component
import java.awt.KeyboardFocusManager
import javax.swing.*

class Screen : JFrame {
    private val panelsToHighlight: MutableList<JComponent> = mutableListOf()

    constructor() {
        this.setSize(900, 600)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.setLocationRelativeTo(null)
        this.title = "Publisher"
        this.init()
    }

    private fun init() {
        this.initTabbedPane()
        this.installFocusHighlight()
    }

    private fun initTabbedPane() {
        val registry: (JComponent) -> Unit = { this.panelsToHighlight.add(it) }
        val tabbedPane = JTabbedPane()
        val build = BuildPanel(registry)
        val publish = PublishPanel(registry)
        tabbedPane.addTab("构建", build)
        tabbedPane.addTab("发布", publish)
        this.add(tabbedPane)
    }

    private fun installFocusHighlight() {
        val originalBorders = this.panelsToHighlight.associateWith { it.border }
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        focusManager.addPropertyChangeListener("focusOwner") { evt ->
            val newFocusOwner = evt.newValue as? Component
            for (panel in this.panelsToHighlight) {
                if (newFocusOwner != null && SwingUtilities.isDescendingFrom(newFocusOwner, panel)) {
                    panel.border = BorderFactory.createLineBorder(Color.BLACK, 1)
                } else {
                    panel.border = originalBorders[panel]
                }
            }
        }
    }

    fun display() {
        this.isVisible = true
    }
}
