import json
import os
from typing import cast

sub_root = None
_path = os.getcwd()
while True:
    if os.path.exists(os.path.join(_path, ".gitignore")):
        sub_root = _path
        break
    else:
        _path = os.path.abspath(os.path.join(_path, ".."))

parent_root = os.path.abspath(os.path.join(sub_root, ".."))
_config = os.path.join(sub_root, "config\\config.json")


def init_config_json():
    if os.path.isfile(_config):
        return
    data: dict[str, object] = {"versions": list(), "token": "*" * 20}
    with open(_config, mode="w", encoding="UTF-8") as file:
        file.write(json.dumps(data, indent=4))


init_config_json()


def get_versions() -> list[str]:
    with open(_config, mode="r", encoding="UTF-8") as file:
        data: dict[str, object] = json.load(file)
        return cast(list[str], data["versions"])


def get_token() -> str:
    with open(_config, mode="r", encoding="UTF-8") as file:
        data: dict[str, object] = json.load(file)
        return str(data["token"])


def get_transfer_station() -> str:
    return create_if_not_exist(os.path.join(sub_root, "transferstation"))


def get_output():
    return create_if_not_exist(os.path.join(get_transfer_station(), "output"))


def get_temp():
    return create_if_not_exist(os.path.join(get_transfer_station(), "temp"))


def get_garbages():
    return create_if_not_exist(os.path.join(get_transfer_station(), "garbages"))


def create_if_not_exist(path):
    if not os.path.exists(path):
        os.mkdir(path)
    return path
