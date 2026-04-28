from typing import Any, Optional
from metacall import metacall_load_from_memory, metacall

class Executer:
    def __init__(self, code: str, lang: str, func: str, args: Optional[list[Any]] = None):
        self.code = code
        self.lang = lang
        self.func = func
        self.args = args or []

    def load(self):
        metacall_load_from_memory(self.lang, self.code)

    def call(self, *args: Any) -> Any:
        return metacall(self.func, *args)

    def exec(self) -> Any:
        self.load()
        return self.call(*self.args)