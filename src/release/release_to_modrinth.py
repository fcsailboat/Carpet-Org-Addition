import json
import os
import shutil

import requests
from requests import Response

from mod_metadata import ModMetadata
from src import config_utils


def get_project():
    project_data: dict = requests.get("https://api.modrinth.com/v2/project/carpet-org-addition").json()
    for key, value in project_data.items():
        print(f"{key} = {value}")


def upload(data: ModMetadata) -> Response:
    """
    将模组上传到 ``Modrinth``
    :param data: 模型元数据
    """
    url = "https://api.modrinth.com/v2/version"
    request_head = {
        "Authorization": config_utils.get_token(),
        "User-Agent": "https://github.com/fcsailboat/Carpet-Org-Addition"
    }
    body = {
        "name": data.get_display_name(),
        "version_number": data.get_version(),
        "version_type": "release",
        "dependencies": get_dependencies(),
        "game_versions": [data.get_game_versions()],
        "loaders": data.get_loader(),
        "project_id": "L0bOPIqR",
        "file_parts": [data.get_file_name()],
        "primary_file": data.get_file_name(),
        "featured": True
    }
    files = {
        "data": (None, json.dumps(body), "application/json"),
        data.get_file_name(): open(data.get_mod_path(), "rb")
    }
    return requests.post(url, headers=request_head, files=files)


def get_dependencies() -> list[dict]:
    """
    获取模组依赖关系
    :return: list[dict[str, str]] 模组的依赖关系
    """
    return [
        # Fabric API
        {
            "project_id": "P7dR8mSH",
            "dependency_type": "required"
        },
        # Carpet
        {
            "project_id": "TQTTVgYE",
            "dependency_type": "required"
        },
    ]


def prepare_for_upload() -> list[ModMetadata]:
    metadata_list: list[ModMetadata] = []
    output = config_utils.get_output()
    files = [os.path.join(os.path.abspath(output), file) for file in os.listdir(output)]
    for file in files:
        metadata_list.append(ModMetadata(file))
    print("发布前检查：")
    for metadata in metadata_list:
        print(metadata)
    print(f"\n确认将以下{len(files)}个模组发布到Modrinth？ Y/N")
    for metadata in metadata_list:
        print(metadata.get_display_name())
    if input().upper() == "Y":
        print("准备发布到Modrinth")
        return metadata_list
    print("中止发布")
    return []


def move_file(file_name: str):
    from_file = os.path.join(config_utils.get_output(), file_name)
    to_file = os.path.join(config_utils.get_garbages(), file_name)
    shutil.move(from_file, to_file)


def release():
    all_metadata = prepare_for_upload()
    if all_metadata:
        count = 0
        total = len(all_metadata)
        for metadatum in all_metadata:
            count += 1
            progress = f"[{count}/{total}]"
            print(f"{progress} 正在将文件{metadatum.get_file_name()}上传到Modrinth")
            # 对齐文本
            print(f"{" " * len(progress)} 名称：{metadatum.get_display_name()}")
            response = upload(metadatum)
            print(f"版本{metadatum.get_version()}已上传，状态码：{response.status_code}")
            move_file(metadatum.get_file_name())
    else:
        print("未发布模组")


if __name__ == '__main__':
    release()
