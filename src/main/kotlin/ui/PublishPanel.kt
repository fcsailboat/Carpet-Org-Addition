package ui

import javax.swing.JComponent

class PublishPanel : SimplePanel {
    constructor(registry: (JComponent) -> Unit) : super(registry) {
        this.init()
    }

    private fun init() {
    }
}