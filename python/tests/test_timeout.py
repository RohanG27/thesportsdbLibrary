import socket
import time

import pytest

from thesportsdb_client import NetworkError, SportsDB


def test_a_silent_server_times_out() -> None:
    # Accepts the connection but never answers: the call must give up, not hang.
    with socket.socket() as server:
        server.bind(("127.0.0.1", 0))
        server.listen()
        port = server.getsockname()[1]
        with SportsDB(base_url=f"http://127.0.0.1:{port}", timeout=0.3, max_retries=0, requests_per_minute=0) as db:
            started = time.monotonic()
            with pytest.raises(NetworkError):
                db.v1.list.sports()
            assert time.monotonic() - started < 5
