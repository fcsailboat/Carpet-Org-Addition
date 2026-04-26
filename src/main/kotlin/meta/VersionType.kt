package meta

enum class VersionType {
    OFFICIAL,
    SNAPSHOT,
    PRE_RELEASE,
    RELEASE_CANDIDATE;

    companion object {
        fun parse(version: String, format: VersionFormats): VersionType {
            if (version.matches(Regex("(\\d+\\.\\d+)")) || version.matches(Regex("(\\d+\\.\\d+\\.\\d+)"))) {
                return OFFICIAL
            }
            if ("pre" in version) {
                return PRE_RELEASE
            }
            if ("rc" in version) {
                return RELEASE_CANDIDATE
            }
            if (format == VersionFormats.OLD_VERSION && version.matches(Regex("\\d+w\\d+[a-z]"))) {
                return SNAPSHOT
            }
            if (format == VersionFormats.NEW_VERSION && "snapshot" in version) {
                return SNAPSHOT
            }
            throw IllegalStateException("Unknown version type")
        }
    }
}