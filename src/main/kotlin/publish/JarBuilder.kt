package publish

import GlobalConfigs
import Publisher
import meta.VersionFormats
import org.eclipse.jgit.api.Git
import util.copyOrReplaceFile
import util.moveOrReplaceFile
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString

class JarBuilder {
    private val branch: String
    private val format: VersionFormats
    private val logger: (String) -> Unit

    constructor(branch: String, logger: (String) -> Unit) {
        this.branch = branch
        this.format = VersionFormats.parse(branch)
        this.logger = logger
    }

    fun run() {
        this.logger("开始")
        this.switch()
        this.archive()
        this.build()
        this.moveFile()
        this.logger("完成")
    }

    private fun moveFile() {
        val files = GlobalConfigs.getBuildOutput().listFiles() ?: throw IllegalArgumentException()
        val list = files.asList().stream()
            .filter { it.isFile }
            .filter { "sources" !in it.name }
            .filter { it.name.endsWith(".jar") }
            .toList()
        if (list.size != 1) {
            throw IllegalStateException("The build product contains multiple files")
        }
        val file = list.last()
        val from = file.toPath()
        val to = File(GlobalConfigs.getStaging(), file.name).toPath()
        this.logger("正在移动文件：${file.name}")
        copyOrReplaceFile(from, to)
        Publisher.LOGGER.info("File ${from.absolutePathString()} has been moved to ${to.absolutePathString()}")
    }

    private fun archive() {
        val output = GlobalConfigs.getBuildOutput()
        val archive = File(output, "archive")
        if (!archive.exists()) {
            archive.mkdirs()
        }
        val files = output.listFiles()?.toList() ?: listOf()
        for (file in files) {
            if (file.isFile) {
                moveOrReplaceFile(file.toPath(), archive.resolve(file.name).toPath())
            }
        }
    }

    /**
     * 切换至当前分支
     */
    private fun switch() {
        this.logger("切换到${this.branch}分支")
        GIT.checkout().setName(this.branch).call()
    }

    private fun build() {
        this.logger("开始构建：${this.branch}")
        var i = 0
        while (true) {
            if (i >= 3) {
                throw IllegalStateException("Attempted an unreasonable number of times")
            }
            i++
            this.logger("正在进行第${i}次尝试")
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
        val processBuilder = ProcessBuilder("cmd", "/c", "gradlew", "build")
        processBuilder.directory(GlobalConfigs.getRoot()).inheritIO()
        Publisher.LOGGER.info("Working directory: ${GlobalConfigs.getRoot()}")
        val process = processBuilder.start()
        // 要求在10分钟内完成，下载依赖可能需要相当长的时间
        val finished = process.waitFor(600, TimeUnit.SECONDS)
        if (finished) {
            val code = process.exitValue()
            if (code == 0) {
                this.logger("构建成功！")
                return
            } else {
                this.logger("构建失败，退出码：$code")
                throw IllegalStateException("[${this.branch}] Build jar failed, exit code: $code")
            }
        }
        process.destroyForcibly()
        throw IllegalStateException("[${this.branch}] Command execution timeout")
    }

    companion object {
        val GIT: Git = Git.open(GlobalConfigs.getRoot())
    }
}
