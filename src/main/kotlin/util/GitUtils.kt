package util

import meta.VersionFormats
import org.eclipse.jgit.api.Git
import java.nio.file.Path
import kotlin.math.max

fun listBranch(path: Path): List<String> {
    val git = Git.open(path.toFile())
    val call = git.branchList().call()
    return call.stream().map { it.name.replace("refs/heads/", "") }.toList()
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
    return list.stream().sorted { s1, s2 ->
        val arr1 = s1.split(".")
        val arr2 = s2.split(".")
        val max = max(arr1.size, arr2.size)
        for (index in 0 until max) {
            val i1 = if (index >= arr1.size) 0 else arr1[index].toInt()
            val i2 = if (index >= arr2.size) 0 else arr2[index].toInt()
            val n = i1.compareTo(i2)
            if (n != 0) {
                return@sorted -n
            }
        }
        return@sorted 0
    }.toList()
}