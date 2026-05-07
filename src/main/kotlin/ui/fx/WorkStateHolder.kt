package ui.fx

class WorkStateHolder<T : Enum<T>> {
    var workState: T
        private set
    private val listeners = ArrayList<(T) -> Unit>()

    constructor(defaultState: T) {
        this.workState = defaultState
    }

    fun changeWorkState(state: T) {
        this.workState = state
        this.listeners.forEach { it(state) }
    }

    fun addChangeListener(listener: (T) -> Unit) {
        this.listeners.add(listener)
    }
}
