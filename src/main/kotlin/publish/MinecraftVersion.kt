package publish

class MinecraftVersion {
    private val type: Int
    private val ordinal: Int
    private val major: Int
    private val minor: Int
    private val fix: Int
    private val version: String

    constructor(version: String) {
        this.version = version
        if (
            version.matches(Regex("\\d+\\.\\d+"))
            || version.matches(Regex("\\d+\\.\\d+\\.\\d+"))
            || version.matches(Regex("\\d+\\.\\d+\\.[xX*]"))
        ) {
            this.type = OFFICIAL
            this.ordinal = 0
            val triple = this.parseOfficialVersion(version)
            this.major = triple.first
            this.minor = triple.second
            this.fix = triple.third
        } else if ("snapshot" in version) {
            this.type = SNAPSHOT
            val split = version.split("-snapshot-")
            this.ordinal = split[1].toInt()
            val triple = this.parseOfficialVersion(split[0])
            this.major = triple.first
            this.minor = triple.second
            this.fix = triple.third
        } else if ("pre" in version) {
            this.type = PRE_RELEASE
            val split = version.split("-pre-")
            this.ordinal = split[1].toInt()
            val triple = this.parseOfficialVersion(split[0])
            this.major = triple.first
            this.minor = triple.second
            this.fix = triple.third
        } else if ("rc" in version) {
            this.type = RELEASE_CANDIDATE
            val split = version.split("-rc-")
            this.ordinal = split[1].toInt()
            val triple = this.parseOfficialVersion(split[0])
            this.major = triple.first
            this.minor = triple.second
            this.fix = triple.third
        } else {
            this.type = UNKNOWN
            this.ordinal = 0
            this.minor = 0
            this.major = 0
            this.fix = 0
        }
    }

    private fun parseOfficialVersion(version: String): Triple<Int, Int, Int> {
        val split = version.split(".")
        val first = split[0].toInt()
        val second = split[1].toInt()
        return if (split.size >= 3) {
            val third = split[2]
            if (third.matches(Regex("[xX*]"))) {
                Triple(first, second, Int.MAX_VALUE)
            } else {
                Triple(first, second, third.toInt())
            }
        } else {
            Triple(first, second, 0)
        }
    }

    fun isValid(): Boolean {
        return this.type != UNKNOWN
    }

    operator fun compareTo(other: MinecraftVersion): Int {
        return compareValuesBy(
            this,
            other,
            { it.major },
            { it.minor },
            { it.fix },
            { it.type },
            { it.ordinal }
        )
    }

    override fun toString(): String {
        return this.version
    }

    companion object {
        private const val OFFICIAL = 1
        private const val SNAPSHOT = 2
        private const val PRE_RELEASE = 3
        private const val RELEASE_CANDIDATE = 4
        private const val UNKNOWN = 10
    }
}