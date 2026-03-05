import json
import os
import re
import zipfile

from src import config_utils

# 模组版本路径
output_path = "..\\..\\libs\\version"


def read_fabric_mod_json(mod_file) -> dict:
    """
    读取模组jar文件的fabric.mod.json
    :rtype: dict 模组元数据字典
    """
    with zipfile.ZipFile(mod_file, "r") as jar:
        with jar.open("fabric.mod.json") as mod_json:
            loads = json.loads(mod_json.read().decode("UTF-8"))
            return loads


def list_file() -> list[str]:
    """
    获取版本目录下的所有文件
    """
    global output_path
    listdir = os.listdir(config_utils.get_output())
    return [os.path.join(config_utils.get_output(), name) for name in listdir]


class ModMetadata:
    __candidate_game_version: str = None

    def __init__(self, mod_file: str):
        data = read_fabric_mod_json(mod_file)
        self.__path = mod_file
        self.__id = data["id"]
        self.__version = data["version"]
        self.__name = data["name"]
        self.__depends = data["depends"]
        self.__loaders = "fabric"
        # 从文件名解析游戏版本
        self.__file_name_as_candidate_version = re.split("[/\\\\]", mod_file)[-1].split("-")[3][2:]

    def get_mod_path(self):
        return self.__path

    def get_file_name(self):
        return os.path.basename(self.__path)

    def get_id(self):
        return self.__id

    def get_version(self):
        return self.__version

    def get_name(self):
        return self.__name

    def get_display_name(self):
        return f"{self.get_id()}-mc{self.get_game_versions()}-{self.get_version()}"

    def get_dependencies(self):
        return self.__depends

    def get_game_versions(self):
        if self.__candidate_game_version:
            return self.__candidate_game_version
        # 普通版本，直接获取并返回
        minecraft_version = self.__depends["minecraft"]
        if re.match("[1-9]\\.[\\d{2}](\\\\d])?", minecraft_version):
            self.__candidate_game_version = minecraft_version
            return minecraft_version
        # 快照版游戏版本为范围，但实际只有一个Minecraft版本可用
        print(f"文件{self.get_file_name()}需要指定版本，输入Y以选择{self.__file_name_as_candidate_version}")
        while self.__candidate_game_version is None:
            designated = input()
            if designated == "y" or designated == "Y":
                # 直接使用候选游戏版本
                self.__candidate_game_version = self.__file_name_as_candidate_version
                break
            if len(designated) < 3:
                print("无效输入，是否误触了回车键？")
            else:
                # 手动指定游戏版本
                self.__candidate_game_version = designated
        return self.__candidate_game_version

    def get_loader(self):
        return [self.__loaders]

    def __str__(self):
        return f"版本：{self.get_version()}，游戏版本：{self.get_game_versions()}，文件名称：{self.get_file_name()}"


if __name__ == '__main__':
    for file in list_file():
        metadata = ModMetadata(file)
        print(metadata.get_file_name())
