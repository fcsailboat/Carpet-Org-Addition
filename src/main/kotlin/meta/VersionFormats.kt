package meta

import java.io.File

enum class VersionFormats {
    OLD_VERSION,
    NEW_VERSION,
    INVALID;

    fun test(file: File, version: String): Boolean {
        if (this == INVALID) {
            return false
        }
        val name = file.name
        if (this == OLD_VERSION) {
            if (name.matches(Regex("carpet-org-addition-mc${version}-v.*-\\d+\\.jar"))) {
                return true
            }
            if (name.matches(Regex("carpet-org-addition-mc\\d+w\\d+[a-z]-v.*-\\d+\\.jar"))) {
                return true
            }
            if (name.matches(Regex("carpet-org-addition-mc${version}-pre\\d+-v.*-\\d+\\.jar"))) {
                return true
            }
            if (name.matches(Regex("carpet-org-addition-mc${version}-rc\\d+-v.*-\\d+\\.jar"))) {
                return true
            }
        } else if (this == NEW_VERSION) {
            if (name.matches(Regex("carpet-org-addition-mc${version}\\.x-v.*-\\d+\\.jar"))) {
                return true
            }
            if (name.matches(Regex("carpet-org-addition-mc${version}(\\.\\d+)?-snapshot-\\d+-v.*-\\d+\\.jar"))) {
                return true
            }
            if (name.matches(Regex("carpet-org-addition-mc${version}(\\.\\d+)?-pre-\\d+-v.*-\\d+\\.jar"))) {
                return true
            }
            if (name.matches(Regex("carpet-org-addition-mc${version}(\\.\\d+)?-rc-\\d+-v.*-\\d+\\.jar"))) {
                return true
            }
        }
        return false
    }

    companion object {
        fun parse(version: String): VersionFormats {
            if (version.matches(Regex("\\d+\\.\\d+")) || version.matches(Regex("\\d+\\.\\d+\\.\\d+"))) {
                return if (version.startsWith("1.")) OLD_VERSION else NEW_VERSION
            }
            return INVALID
        }
    }
}
