"""API smoke tests for the FastAPI prototype."""

import importlib
import os
import sys
import tempfile

_BACKEND_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _client_for_temp_db():
    fd, path = tempfile.mkstemp(suffix=".db")
    os.close(fd)
    os.environ["DATABASE_URL"] = f"sqlite:///{path}"
    if "main" in sys.modules:
        del sys.modules["main"]
    sys.path.insert(0, _BACKEND_DIR)
    main = importlib.import_module("main")
    main.Base.metadata.create_all(bind=main.engine)
    from fastapi.testclient import TestClient

    return TestClient(main.app), main


def test_health_and_project_flow():
    client, _ = _client_for_temp_db()
    health = client.get("/health")
    assert health.status_code == 200
    assert health.json()["status"] == "ok"

    project = client.post(
        "/projects",
        json={"name": "Test Project", "description": "demo"},
    )
    assert project.status_code == 200
    project_id = project.json()["id"]

    client.patch(
        f"/projects/{project_id}",
        json={"description": "updated"},
    ).raise_for_status()

    context = client.get(f"/projects/{project_id}/context")
    assert context.status_code == 200
    assert "Test Project" in context.json()["context"]

    req = client.post(
        f"/projects/{project_id}/requirements",
        json={"title": "Login", "description": "Users sign in"},
    ).json()
    client.patch(
        f"/requirements/{req['id']}",
        json={"description": "Users sign in securely"},
    ).raise_for_status()

    task = client.post(
        f"/projects/{project_id}/tasks",
        json={"title": "Build login", "priority": "high"},
    ).json()
    client.delete(f"/tasks/{task['id']}").raise_for_status()

    doc = client.post(
        f"/projects/{project_id}/documents",
        json={"title": "Auth README", "body": ""},
    ).json()
    generated = client.post(f"/documents/{doc['id']}/ai/generate")
    assert generated.status_code == 200
    assert generated.json()["body"]

    chat = client.post(
        f"/projects/{project_id}/chat/developer",
        json={"message": "What should we implement first?"},
    )
    assert chat.status_code == 200
    assert chat.json()["reply"]

    client.delete(f"/requirements/{req['id']}").raise_for_status()
    client.delete(f"/documents/{doc['id']}").raise_for_status()
