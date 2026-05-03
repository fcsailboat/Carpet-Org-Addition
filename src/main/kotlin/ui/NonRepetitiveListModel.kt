package ui

import javax.swing.DefaultListModel
import javax.swing.ListModel

class NonRepetitiveListModel<E>
private constructor(private val trustee: DefaultListModel<E>) :
    ListModel<E> by trustee, Iterable<E> {
    private val deduplicator = HashSet<E>()

    constructor() : this(DefaultListModel<E>())

    fun add(element: E) {
        if (this.deduplicator.add(element)) {
            this.trustee.addElement(element)
        }
    }

    fun add(index: Int, element: E) {
        if (this.deduplicator.add(element)) {
            this.trustee.add(index, element)
        }
    }

    fun addAll(versions: Collection<E>) {
        this.trustee.addAll(versions.filter { this.deduplicator.add(it) })
    }

    fun removeAt(index: Int) {
        val e = this.trustee.elementAt(index)
        this.deduplicator.remove(e)
        this.trustee.remove(index)
    }

    fun get(index: Int): E {
        return this.trustee.get(index)
    }

    fun sort(from: Int, to: Int, comparator: Comparator<E>) {
        if (from !in 0..to || to >= this.size) {
            throw IllegalArgumentException("range [$from, $to] invalid for size ${this.size}")
        }
        if (from == to) {
            return
        }
        val list = (from..to).map { this.trustee[it] }.toMutableList()
        list.sortWith(comparator)
        for ((index, e) in list.withIndex()) {
            this.trustee[from + index] = e
        }
    }

    fun clear() {
        this.deduplicator.clear()
        this.trustee.clear()
    }

    override fun iterator(): Iterator<E> {
        return this.trustee.elements().asIterator()
    }
}
