package ui.fx

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox

class SortableListCell<T>(
    private val checkStates: MutableMap<T, BooleanProperty>,
    private val nameSupplier: (T) -> String
) : ListCell<T>() {
    private val checkBox = CheckBox()
    private val label = Label()
    private val pane = HBox(5.0, this.checkBox, this.label)
    private var insertIndicator: InsertIndicator = InsertIndicator.NONE
    private var currentProperty: BooleanProperty? = null
    private var draggedItem: T? = null

    init {
        this.pane.alignment = Pos.CENTER_LEFT
        this.setOnDragDetected { event ->
            if (this.isEmpty) {
                return@setOnDragDetected
            }
            this.draggedItem = this.item
            val dragboard = this.startDragAndDrop(TransferMode.MOVE)
            val content = ClipboardContent()
            content.putString(this.index.toString())
            dragboard.setContent(content)
            event.consume()
        }
        this.setOnDragOver { event ->
            if (event.gestureSource != this && event.dragboard.hasString()) {
                event.acceptTransferModes(TransferMode.MOVE)
                val height = this.height
                val mouseY = event.y
                val newIndicator = if (mouseY < height / 2) InsertIndicator.ABOVE else InsertIndicator.BELOW
                if (newIndicator != this.insertIndicator) {
                    this.insertIndicator = newIndicator
                    this.updateInsertIndicatorStyle()
                }
            }
            event.consume()
        }
        this.setOnDragEntered { event ->
            if (event.gestureSource != this && event.dragboard.hasString()) {
                val mouseY = event.y
                this.insertIndicator = if (mouseY < this.height / 2) InsertIndicator.ABOVE else InsertIndicator.BELOW
                this.updateInsertIndicatorStyle()
            }
            event.consume()
        }
        this.setOnDragExited { event ->
            this.insertIndicator = InsertIndicator.NONE
            this.updateInsertIndicatorStyle()
            event.consume()
        }
        this.setOnDragDropped { event ->
            val dragboard = event.dragboard
            var success = false
            if (dragboard.hasString()) {
                val listView = this.listView
                if (listView != null) {
                    val sourceIndex = dragboard.string.toIntOrNull() ?: -1
                    val targetIndex = if (this.insertIndicator == InsertIndicator.ABOVE) this.index else this.index + 1
                    if (sourceIndex in listView.items.indices && sourceIndex != targetIndex) {
                        val item = listView.items[sourceIndex]
                        listView.items.removeAt(sourceIndex)
                        val adjustedTarget = if (sourceIndex < targetIndex) targetIndex - 1 else targetIndex
                        listView.items.add(adjustedTarget, item)
                        listView.selectionModel.select(adjustedTarget)
                        success = true
                    }
                }
            }
            event.isDropCompleted = success
            this.insertIndicator = InsertIndicator.NONE
            event.consume()
        }
        this.setOnDragDone { event ->
            this.insertIndicator = InsertIndicator.NONE
            this.updateInsertIndicatorStyle()
            this.draggedItem = null
            event.consume()
        }
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        this.currentProperty?.let {
            this.checkBox.selectedProperty().unbindBidirectional(it)
        }
        this.currentProperty = null
        if (empty || item == null) {
            this.graphic = null
            this.insertIndicator = InsertIndicator.NONE
            this.updateInsertIndicatorStyle()
            this.text = null
        } else {
            val prop = checkStates.getOrPut(item) { SimpleBooleanProperty(false) }
            this.checkBox.selectedProperty().bindBidirectional(prop)
            this.currentProperty = prop
            this.label.text = this.nameSupplier(item)
            this.graphic = this.pane
            this.insertIndicator = InsertIndicator.NONE
            this.updateInsertIndicatorStyle()
            this.text = null
        }
    }

    private fun updateInsertIndicatorStyle() {
        when (this.insertIndicator) {
            InsertIndicator.ABOVE -> {
                this.style = "-fx-border-color: dodgerblue; -fx-border-width: 2 0 0 0; -fx-border-style: solid;"
            }

            InsertIndicator.BELOW -> {
                this.style = "-fx-border-color: dodgerblue; -fx-border-width: 0 0 2 0; -fx-border-style: solid;"
            }

            InsertIndicator.NONE -> {
                this.style = ""
            }
        }
    }

    private enum class InsertIndicator {
        NONE, ABOVE, BELOW
    }
}