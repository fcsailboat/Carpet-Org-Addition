package ui.fx

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.layout.VBox.setVgrow
import javafx.stage.Stage

class MainScreen : Application() {
    override fun start(stage: Stage) {
        val root = VBox()
        this.addTabPane(root)
        val scene = Scene(root, 960.0, 650.0)
        stage.title = "Publisher"
        stage.scene = scene
        stage.show()
    }

    private fun addTabPane(box: VBox) {
        val tabPane = TabPane()
        setVgrow(tabPane, Priority.ALWAYS)
        tabPane.tabs.add(Tab("构建", BuildTab()))
        tabPane.tabs.add(Tab("发布", PublishTab()))
        tabPane.tabs.add(Tab("合并分支", MergeBranchTab()))
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        tabPane.tabDragPolicy = TabPane.TabDragPolicy.REORDER
        box.children.add(tabPane)
    }
}
