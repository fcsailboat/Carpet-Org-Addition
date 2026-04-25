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
            if (version.contains("pre")) {
                return PRE_RELEASE
            }
            if (version.contains("rc")) {
                return RELEASE_CANDIDATE
            }
            if (format == VersionFormats.OLD_VERSION && version.matches(Regex("\\d+w\\d+[a-z]"))) {
                return SNAPSHOT
            }
            if (format == VersionFormats.NEW_VERSION && version.contains("snapshot")) {
                return SNAPSHOT
            }
            throw IllegalStateException("Unknown version type")
        }
    }
}