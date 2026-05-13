package publish

import meta.VersionFormats
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Ref

class Branch {
    private val git: Git
    private val ref: Ref
    val name: String

    private constructor(git: Git, ref: Ref) {
        this.git = git
        this.ref = ref
        this.name = ref.name.replace("refs/heads/", "")
    }

    private fun isValidBranch(): Boolean {
        return VersionFormats.parse(this.name) != VersionFormats.INVALID
    }

    fun acceptMerge(other: Branch, logger: (String) -> Unit) {
        if (!this.git.status().call().isClean) {
            throw IllegalStateException("无法签出分支，工作区存在未处理更改")
        }
        if (this.name == other.name) {
            throw IllegalArgumentException("分支不应与自身合并")
        }
        this.git.checkout().setName(this.name).call()
        var reset = true
        try {
            val result = this.git.merge()
                .include(other.ref)
                .call()
            when (result.mergeStatus) {
                MergeResult.MergeStatus.FAST_FORWARD -> {
                    logger("已将${other.name}合并到${this.name}")
                }

                MergeResult.MergeStatus.ALREADY_UP_TO_DATE -> {
                    logger("${this.name}已是最新状态")
                }

                MergeResult.MergeStatus.MERGED -> {
                    logger("已将${other.name}合并到${this.name}")
                }

                MergeResult.MergeStatus.FAST_FORWARD_SQUASHED, MergeResult.MergeStatus.MERGED_SQUASHED, MergeResult.MergeStatus.MERGED_SQUASHED_NOT_COMMITTED -> {
                    throw BranchMergeException(this.name, other.name, "已合并，但未提交")
                }

                MergeResult.MergeStatus.CONFLICTING -> {
                    throw BranchMergeException(this.name, other.name, "合并冲突，需手动处理")
                }

                MergeResult.MergeStatus.ABORTED, MergeResult.MergeStatus.FAILED, MergeResult.MergeStatus.NOT_SUPPORTED -> {
                    throw BranchMergeException(this.name, other.name, "合并失败")
                }

                MergeResult.MergeStatus.MERGED_NOT_COMMITTED -> {
                    throw BranchMergeException(this.name, other.name, "可以合并，但无法提交")
                }

                MergeResult.MergeStatus.CHECKOUT_CONFLICT -> {
                    reset = false
                    throw BranchMergeException(
                        this.name,
                        other.name,
                        "存在签出冲突，检查工作区状态，提交或暂存本地修改后重试"
                    )
                }
            }
        } catch (e: Exception) {
            if (reset) {
                // 一旦失败，立即回退，不保存状态，失败状态可以通过再次尝试外部执行Git命令合并来重新复现
                this.git.reset().setMode(ResetCommand.ResetType.HARD).call()
                logger("已回退合并！")
            }
            throw e
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as Branch
        if (git != other.git) {
            return false
        }
        if (name != other.name) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = git.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }


    companion object {
        fun listLocalBranch(git: Git): List<Branch> {
            return try {
                git.branchList()
                    .call()
                    .stream()
                    .filter { it.name.startsWith("refs/heads/") }
                    .map { Branch(git, it) }
                    .filter { it.isValidBranch() }
                    .toList()
            } catch (_: RepositoryNotFoundException) {
                listOf()
            }
        }
    }
}