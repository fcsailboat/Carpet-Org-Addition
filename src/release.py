def get_token() -> str:
    file = open("..\\libs\\token.txt")
    token = file.readline()
    file.close()
    return token
