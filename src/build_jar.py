import os
import re
import shutil
import subprocess
from subprocess import TimeoutExpired, CompletedProcess

from git import Repo

from src import config_utils
from src.config_utils import parent_root

# 模组输出目录
output_path = config_utils.get_output()
# 构建出模组的数量
version_count = 0
repo = Repo(parent_root)


def latest_file(files: list[str], branch: str):
    """获取最新的模组文件
    :param files:
    :param branch:
    :return:最新构建的模组文件的绝对路径
    """
    jar_files = [file for file in files if is_jar_file(branch, file)]
    return max(jar_files, key=str)


def is_jar_file(branch: str, file: str):
    """
    指定文件是否为当前分支的模组文件
    :param branch:Git分支的名称
    :param file:文件的绝对路径
    """
    return re.match(f"carpet-org-addition-mc{branch}-v.*-\\d+\\.jar", os.path.basename(file))


def copy_the_latest_file(branch: str):
    """
    将最新构建的文件复制到version文件夹
    :param branch:
    """
    global output_path
    path = os.path.join(parent_root, "build\\libs")
    files = [os.path.join(path, file) for file in os.listdir(path)]
    last = latest_file(files, branch)
    print(f"正在将文件{os.path.basename(last)}复制到{path}")
    shutil.copy(last, output_path)


def build(branch: str):
    """
    构建当前分支的模组jar
    """
    count = 0
    while True:
        if count >= 3:
            raise RuntimeError("重试次数过多")
        count += 1
        try:
            print(f"正在进行第{count}次尝试")
            result: CompletedProcess[bytes] = subprocess.run(
                args=["gradlew", "remapJar"],
                timeout=600,
                cwd=parent_root,
                shell=True
            )
            return_code = result.returncode
            if return_code == 0:
                copy_the_latest_file(branch)
                global version_count
                version_count += 1
            else:
                print(f"第{count}次构建{branch}失败，返回码：{return_code}")
                continue
        except TimeoutExpired:
            print("操作超时，正在重试")
            continue
        break
    print("完成构建")


def switch_branch(breach: str):
    """
    切换到指定分支
    :param breach: 分支名称
    """
    repo.git.checkout(breach)


def build_all_version_jar():
    """
    构建所有版本
    """
    branches = config_utils.get_versions()
    for branch in branches:
        print(f"正在切换到{branch}分支")
        switch_branch(branch)
        print(f"正在构建{branch}版本")
        build(branch)
        print("-" * 70)
    global version_count
    if version_count != len(branches):
        raise RuntimeError(f"构建模组的数量与预期不匹配：\n\t预期：{len(branches)}\n\t实际：{version_count}")


def check_file_directory():
    """
    检查输出目录中是否有其他文件
    """
    if not os.path.exists(output_path):
        os.mkdir(output_path)
        return
    if len(os.listdir(output_path)) == 0:
        return
    raise RuntimeError("输出目录非空")


def start():
    check_file_directory()
    build_all_version_jar()


if __name__ == '__main__':
    start()
