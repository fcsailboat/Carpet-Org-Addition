package ui.fx

import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.util.Callback

class WritableUniqueListView<T> : VBox() {
    private val observableList: ObservableList<T> = FXCollections.observableArrayList()
    private val listView: ListView<T> = ListView(this.observableList)
    private val deduplicator = HashSet<T>()
    val contextMenu: ContextMenu
        get() {
            val view = this.listView
            if (view.contextMenu == null) {
                view.contextMenu = ContextMenu()
            }
            return view.contextMenu
        }
    var selectionModel: MultipleSelectionModel<T>?
        get() = this.listView.selectionModel
        set(value) {
            this.listView.selectionModel = value
        }
    val size: Int get() = this.observableList.size
    var cellFactory: Callback<ListView<T>, ListCell<T>>
        get() = this.listView.cellFactory
        set(value) {
            this.listView.cellFactory = value
        }

    init {
        this.children.add(this.listView)
    }

    fun add(element: T) {
        if (this.deduplicator.add(element)) {
            this.observableList.add(element)
        }
    }

    fun addAll(elements: Collection<T>) {
        val list = ArrayList<T>(elements.size)
        for (element in elements) {
            if (this.deduplicator.add(element)) {
                list.add(element)
            }
        }
        this.observableList.addAll(list)
    }

    fun clear() {
        this.deduplicator.clear()
        this.observableList.clear()
    }

    operator fun contains(element: T): Boolean {
        return element in this.deduplicator
    }

    fun addListChangeListener(listener: ListChangeListener<T>) {
        this.observableList.addListener(listener)
    }

    fun addContextMenu(item: MenuItem) {
        this.contextMenu.items.add(item)
    }

    fun remove(element: T) {
        if (this.observableList.remove(element)) {
            this.deduplicator.remove(element)
        }
    }
}
