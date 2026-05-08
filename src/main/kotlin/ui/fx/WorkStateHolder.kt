package ui.fx

import java.util.concurrent.atomic.AtomicBoolean

class WorkStateHolder<T : Enum<T>> {
    var workState: T
        private set
    private val stopFlag = AtomicBoolean(false)
    var cancel: Boolean
        get() = this.stopFlag.get()
        set(value) = this.stopFlag.set(value)
    private val listeners = ArrayList<(T) -> Unit>()
    private val validators = ArrayList<(T) -> Boolean>()

    constructor(defaultState: T) {
        this.workState = defaultState
    }

    fun changeWorkState(state: T) {
        if (this.validators.stream().allMatch { it(state) }) {
            this.workState = state
            this.listeners.forEach { it(state) }
        }
    }

    fun addChangeValidator(validator: (T) -> Boolean) {
        this.validators.add(validator)
    }

    fun addChangeListener(listener: (T) -> Unit) {
        this.listeners.add(listener)
    }
}
