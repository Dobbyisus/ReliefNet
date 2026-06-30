from pathlib import Path
import shutil


SOURCE = Path(
    r"C:\Users\Shashwat Tiwari\.codex\plugins\cache\openai-primary-runtime\documents\26.623.12021\skills\documents\render_docx.py"
)
TARGET = Path(__file__).resolve().parent / "_render_docx_impl.py"

shutil.copyfile(SOURCE, TARGET)
print(TARGET)
