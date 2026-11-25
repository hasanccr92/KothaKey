import re
from pathlib import Path

src = Path(r"F:\keyboard\AnySoftKeyboard\addons\languages\bengali_avro\pack\dictionary\prebuilt\bengali.xml")
dst = Path(r"F:\keyboard\KothaKey\app\src\main\assets\bengali_words.txt")
dst.parent.mkdir(parents=True, exist_ok=True)
text = src.read_text(encoding="utf-8")
words = re.findall(r"<w f=\"\d+\">(.*?)</w>", text)
dst.write_text("\n".join(words) + "\n", encoding="utf-8")
print(f"{len(words)} words -> {dst} ({dst.stat().st_size} bytes)")
