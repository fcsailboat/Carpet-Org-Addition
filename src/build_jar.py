import os
import shutil
import subprocess
from subprocess import TimeoutExpired

from git import Repo

# 项目Git根目录
directory = os.path.abspath(os.path.join(os.getcwd(), "..\\.."))
# 模组输出目录
version_path = os.path.join(directory, "python\\libs\\version")
# 构建出模组的数量
version_count = 0
repo = Repo(directory)


def copy_the_latest_file():
    global directory, version_path
    path = os.path.join(directory, "build\\libs")
    files = [os.path.join(path, file) for file in os.listdir(path) if file not in "source"]
    last = max(files, key=os.path.getmtime)
    print(f"正在将文件{os.path.basename(last)}复制到{path}")
    shutil.copy(last, version_path)


def build_jar():
    """
    构建当前分支的模组jar
    """
    count = 0
    while True:
        count += 1
        try:
            print(f"正在进行第{count}次尝试")
            subprocess.run(args=["gradlew", "remapJar"], timeout=60, cwd=directory, shell=True)
            copy_the_latest_file()
            global version_count
            version_count += 1
        except TimeoutExpired as e:
            print("操作超时，正在重试")
            if count >= 3:
                raise e
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
    branches = ["1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6"]
    for branch in branches:
        print(f"正在切换到{branch}分支")
        switch_branch(branch)
        print(f"正在构建{branch}版本")
        build_jar()
        print("-" * 70)
    global version_count
    if version_count != len(branches):
        raise RuntimeError(f"构建模组的数量与预期不匹配：\n\t预期：{len(branches)}\n\t实际：{version_count}")


def check_file_directory():
    """
    检查输出目录中是否有其他文件
    """
    if len(os.listdir(version_path)) == 0:
        return
    raise RuntimeError("输出目录非空")


if __name__ == '__main__':
    check_file_directory()
    build_all_version_jar()
