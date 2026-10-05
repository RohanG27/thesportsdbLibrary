import subprocess
import sys
from pathlib import Path


def test_sync_client_is_generated_from_the_async_source() -> None:
    tool = Path(__file__).resolve().parent.parent / "tools" / "unasync.py"
    result = subprocess.run([sys.executable, str(tool), "--check"], capture_output=True, text=True)
    assert result.returncode == 0, result.stdout
