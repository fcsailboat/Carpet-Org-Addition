package ui.fx

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.css.PseudoClass
import javafx.geometry.Pos
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox

class SortableListCell<T>(
    private val checkStates: MutableMap<T, BooleanProperty>,
    private val nameSupplier: (T) -> String,
    private val maxValidIndex: () -> Int
) : ListCell<T>() {
    private val checkBox = CheckBox()
    private val label = Label()
    private val pane = HBox(5.0, this.checkBox, this.label)
    private var insertIndicator: InsertIndicator = InsertIndicator.NONE
    private var currentProperty: BooleanProperty? = null
    private var strikethroughListener: ChangeListener<Boolean>? = null

    init {
        this.pane.alignment = Pos.CENTER_LEFT
        this.setOnDragDetected { event ->
            if (this.isEmpty) {
                return@setOnDragDetected
            }
            val dragboard = this.startDragAndDrop(TransferMode.MOVE)
            val content = ClipboardContent()
            content.putString(this.index.toString())
            dragboard.setContent(content)
            event.consume()
        }
        this.setOnDragOver { event ->
            val gestureSourceIndex = this.gestureSourceIndex(event)
            if (event.gestureSource != this && event.dragboard.hasString() && isDragValid(gestureSourceIndex)) {
                event.acceptTransferModes(TransferMode.MOVE)
                val height = this.height
                val mouseY = event.y
                val newIndicator = if (mouseY < height / 2) InsertIndicator.ABOVE else InsertIndicator.BELOW
                if (newIndicator != this.insertIndicator) {
                    this.insertIndicator = newIndicator
                    this.updateInsertIndicatorStyle(gestureSourceIndex)
                }
            }
            event.consume()
        }
        this.setOnDragEntered { event ->
            if (event.gestureSource != this && event.dragboard.hasString()) {
                val mouseY = event.y
                this.insertIndicator = if (mouseY < this.height / 2) InsertIndicator.ABOVE else InsertIndicator.BELOW
                this.updateInsertIndicatorStyle(this.gestureSourceIndex(event))
            }
            event.consume()
        }
        this.setOnDragExited { event ->
            this.insertIndicator = InsertIndicator.NONE
            this.updateInsertIndicatorStyle(this.gestureSourceIndex(event))
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
            this.updateInsertIndicatorStyle(this.gestureSourceIndex(event))
            event.consume()
        }
    }

    private fun gestureSourceIndex(event: DragEvent): Int {
        return event.dragboard.string?.toIntOrNull() ?: -1
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        this.currentProperty?.let { property ->
            this.checkBox.selectedProperty().unbindBidirectional(property)
            this.strikethroughListener?.let { property.removeListener(it) }
        }
        this.currentProperty = null
        this.strikethroughListener = null
        this.text = null
        if (empty || item == null) {
            this.graphic = null
            this.insertIndicator = InsertIndicator.NONE
            this.label.pseudoClassStateChanged(STRIKETHROUGH_PSEUDO, false)
        } else {
            val property = checkStates.getOrPut(item) { SimpleBooleanProperty(false) }
            this.checkBox.selectedProperty().bindBidirectional(property)
            this.currentProperty = property
            this.label.text = this.nameSupplier(item)
            val listener = ChangeListener<Boolean> { _, _, newValue ->
                this.label.pseudoClassStateChanged(STRIKETHROUGH_PSEUDO, !newValue)
            }
            property.addListener(listener)
            this.label.pseudoClassStateChanged(STRIKETHROUGH_PSEUDO, !property.value)
            this.strikethroughListener = listener
            this.graphic = this.pane
            this.insertIndicator = InsertIndicator.NONE
        }
        this.resetInsertIndicatorStyle()
    }

    private fun updateInsertIndicatorStyle(gestureSourceIndex: Int) {
        when (if (this.isDragValid(gestureSourceIndex)) this.insertIndicator else InsertIndicator.NONE) {
            InsertIndicator.ABOVE -> {
                this.style = "-fx-border-color: dodgerblue; -fx-border-width: 2 0 0 0; -fx-border-style: solid;"
            }

            InsertIndicator.BELOW -> {
                this.style = "-fx-border-color: dodgerblue; -fx-border-width: 0 0 2 0; -fx-border-style: solid;"
            }

            InsertIndicator.NONE -> {
                resetInsertIndicatorStyle()
            }
        }
    }

    private fun resetInsertIndicatorStyle() {
        this.style = ""
    }

    private fun isDragValid(gestureSourceIndex: Int): Boolean {
        if (gestureSourceIndex == -1) {
            return false
        }
        return this.index <= this.maxValidIndex() && gestureSourceIndex <= this.maxValidIndex()
    }

    private enum class InsertIndicator {
        NONE, ABOVE, BELOW
    }

    companion object {
        private val STRIKETHROUGH_PSEUDO = PseudoClass.getPseudoClass("strikethrough")
    }
}