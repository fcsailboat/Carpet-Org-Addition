package publish

import GlobalConfigs
import Publisher
import meta.VersionFormats
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString

class JarBuilder {
    private val branch: String
    private val format: VersionFormats

    constructor(branch: String) {
        this.branch = branch
        this.format = VersionFormats.parse(branch)
    }

    private fun run() {
        this.switch()
        this.build()
        this.moveFile()
    }

    private fun moveFile() {
        val files = GlobalConfigs.getBuildOutput().listFiles() ?: throw IllegalArgumentException()
        val file = files.asList().stream()
            .filter { this.format.test(it, this.branch) }
            .sorted(Comparator.comparingLong {
                val path = it.toPath()
                val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
                return@comparingLong attributes.creationTime().toMillis()
            })
            .toList()
            .last()
        val from = file.toPath()
        val to = File(GlobalConfigs.getStaging(), file.name).toPath()
        Files.copy(from, to)
        Publisher.LOGGER.info("File ${from.absolutePathString()} has been moved to ${to.absolutePathString()}")
    }

    /**
     * 切换至当前分支
     */
    private fun switch() {
        GIT.checkout().setName(this.branch).call()
    }

    private fun build() {
        var i = 0
        while (true) {
            if (i >= 3) {
                throw IllegalStateException("Attempted an unreasonable number of times")
            }
            i++
            Publisher.LOGGER.info("[${this.branch}] The $i attempt is currently underway")
            try {
                this.tryBuild()
                Publisher.LOGGER.info("[${this.branch}] Completed")
                return
            } catch (e: Exception) {
                Publisher.LOGGER.warn("[${this.branch}] Build failed:", e)
                continue
            }
        }
    }

    private fun tryBuild() {
        val processBuilder = ProcessBuilder("cmd", "/c", "gradlew", "publish")
        processBuilder.directory(GlobalConfigs.getRoot()).inheritIO()
        Publisher.LOGGER.info("Working directory: ${GlobalConfigs.getRoot()}")
        val process = processBuilder.start()
        // 要求在10分钟内完成，下载依赖可能需要相当长的时间
        val finished = process.waitFor(600, TimeUnit.SECONDS)
        if (finished) {
            val code = process.exitValue()
            if (code == 0) {
                return
            } else {
                throw IllegalStateException("[${this.branch}] Build jar failed, exit code: $code")
            }
        }
        process.destroyForcibly()
        throw IllegalStateException("[${this.branch}] Command execution timeout")
    }

    companion object {
        val GIT: Git = Git.open(GlobalConfigs.getRoot())

        fun start() {
            check()
            val versions: List<String> = GlobalConfigs.getVersions()
            for (version in versions) {
                Publisher.LOGGER.info(version)
                val builder = JarBuilder(version)
                builder.run()
            }
        }

        private fun check() {
            val file = GlobalConfigs.getStaging()
            if (!file.isDirectory()) {
                file.mkdirs()
            }
            val files = file.listFiles()
            if (files?.isEmpty() ?: false) {
                return
            }
            throw IllegalStateException("'$file' directory is not empty")
        }

    }
}
