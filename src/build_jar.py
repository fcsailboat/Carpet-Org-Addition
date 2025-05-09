import os
import shutil
import subprocess
from subprocess import TimeoutExpired

from git import Repo

directory = os.path.abspath(os.path.join(os.getcwd(), "..\\.."))
repo = Repo(directory)


def copy_the_latest_file():
    global directory
    path = os.path.join(directory, "build\\libs")
    files = [os.path.join(path, file) for file in os.listdir(path) if file not in "source"]
    last = max(files, key=os.path.getmtime)
    shutil.copy(last, os.path.join(directory, "python\\libs\\version"))


def build_jar():
    """
    构建当前分支的模组jar
    """
    count = 0
    while True:
        count += 1
        try:
            print(f"正在进行第{count}次尝试")
            subprocess.run(args=["gradlew", "remapJar"], timeout=10, cwd=directory, shell=True)
            copy_the_latest_file()
        except TimeoutExpired as e:
            if count >= 3:
                raise e
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


if __name__ == '__main__':
    build_all_version_jar()
