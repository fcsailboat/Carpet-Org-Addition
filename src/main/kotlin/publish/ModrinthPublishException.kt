package publish

class ModrinthPublishException : RuntimeException {
    val code: Int

    constructor(code: Int, message: String?) : super(message) {
        this.code = code
    }

    override val message: String
        get() = super.message ?: "None"
}