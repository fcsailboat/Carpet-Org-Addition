package ui.fx

import javafx.animation.*
import javafx.scene.control.Button
import javafx.scene.control.ProgressBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.util.Duration

class CooldownButton : HBox {
    private val pane: StackPane = StackPane()
    private var animation: Timeline? = null
    private val playing: Boolean get() = this.animation?.status == Animation.Status.RUNNING
    private val progressBar = ProgressBar(1.0).apply {
        isMouseTransparent = true
        style = "-fx-accent: rgba(0,0,0,0.25);"
    }
    private val button: Button

    constructor(button: Button) {
        this.button = button
        this.progressBar.prefWidthProperty().bind(button.widthProperty())
        this.progressBar.maxWidthProperty().bind(button.widthProperty())
        this.progressBar.prefHeightProperty().bind(button.heightProperty())
        this.progressBar.maxHeightProperty().bind(button.heightProperty())
        this.progressBar.isVisible = false
        setHgrow(this.pane, Priority.ALWAYS)
        this.pane.children.addAll(this.button, this.progressBar)
        this.children.add(this.pane)
        this.styleClass.add("cooldown-progress")
    }

    fun startCooldown(seconds: Double = 1.0) {
        if (this.playing) {
            return
        }
        this.button.isDisable = true
        this.progressBar.progress = 1.0
        this.progressBar.isVisible = true
        this.animation = Timeline(
            KeyFrame(
                Duration.seconds(seconds), { onCooldownFinished() },
                KeyValue(this.progressBar.progressProperty(), 0.0, Interpolator.EASE_OUT)
            )
        ).apply {
            this.cycleCount = 1
            play()
        }
    }

    fun finishCooldown() {
        this.animation?.apply {
            stop()
            onCooldownFinished()
        }
    }

    private fun onCooldownFinished() {
        this.button.isDisable = false
        this.progressBar.isVisible = false
        this.progressBar.progress = 0.0
    }
}
