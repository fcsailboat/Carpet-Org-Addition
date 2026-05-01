package util

import AppConfiguration
import meta.VersionFormats
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.math.max

fun listBranch(path: Path): List<String> {
    if (path.resolve(".git").isDirectory()) {
        try {
            val git = Git.open(path.toFile())
            val call = git.branchList().call()
            return call.stream().map { it.name.replace("refs/heads/", "") }.toList()
        } catch (_: RepositoryNotFoundException) {
            return listOf()
        }
    }
    return listOf()
}

fun listVersion(path: Path): List<String> {
    val branches = listBranch(path)
    val list = ArrayList<String>(branches.size)
    for (branch in branches) {
        if (VersionFormats.parse(branch) == VersionFormats.INVALID) {
            continue
        }
        list.add(branch)
    }
    return list.stream().sorted { s1, s2 -> versionCompare(s1, s2) }.toList()
}

fun versionCompare(s1: String, s2: String): Int {
    val arr1 = s1.split(".")
    val arr2 = s2.split(".")
    val max = max(arr1.size, arr2.size)
    for (index in 0 until max) {
        val i1 = if (index >= arr1.size) 0 else arr1[index].toInt()
        val i2 = if (index >= arr2.size) 0 else arr2[index].toInt()
        val n = i1.compareTo(i2)
        if (n != 0) {
            return -n
        }
    }
    return 0
}

/**
 * 移动文件，如果目标文件存在，比较文件内容，相同则覆盖，否则抛出异常
 */
fun moveOrReplaceFile(from: Path, to: Path) {
    try {
        Files.move(from, to)
    } catch (e: FileAlreadyExistsException) {
        if (fileEquivalent(from, to)) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING)
        } else {
            throw e
        }
    }
}

fun copyOrReplaceFile(from: Path, to: Path) {
    try {
        Files.copy(from, to)
    } catch (e: FileAlreadyExistsException) {
        if (fileEquivalent(from, to)) {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
        } else {
            throw e
        }
    }
}

fun archiveStagingFile(file: File) {
    moveOrReplaceFile(file.toPath(), AppConfiguration.getArchive().toPath().resolve(file.name))
}

private fun fileEquivalent(path1: Path, path2: Path): Boolean {
    if (Files.mismatch(path1, path2) == -1L) {
        return true
    }
    return try {
        // 比较两个压缩包内的内容是否相同，字节不完全相同可能是因为压缩包内文件的属性不相同
        zipContentAsByteArrays(path1) == zipContentAsByteArrays(path2)
    } catch (_: Exception) {
        false
    }
}

private fun zipContentAsByteArrays(zip: Path): List<Any> {
    val list = ArrayList<ByteArray>()
    ZipInputStream(zip.inputStream().buffered()).use {
        var entry: ZipEntry?
        while (true) {
            entry = it.nextEntry
            if (entry == null) {
                return list.stream().map { bytes -> EqualityByteArray(bytes) }.toList()
            }
            list.add(entry.name.toByteArray(Charsets.UTF_8))
            list.add(it.readAllBytes())
        }
    }
}

private class EqualityByteArray(arr: ByteArray) {
    private val bytes: ByteArray = arr

    override operator fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other === null || this.javaClass !== other.javaClass) {
            return false
        }
        return other is EqualityByteArray && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}