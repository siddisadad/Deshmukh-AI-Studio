"""
AI Studio for Software Engineering — FastAPI backend.

Layered modular monolith: routes → context builder / ask_assistant → AIProvider → SQLite.
"""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from sqlalchemy import (
    Column,
    DateTime,
    ForeignKey,
    Integer,
    String,
    Text,
    create_engine,
)
from sqlalchemy.orm import Session, declarative_base, relationship, sessionmaker

from ai_provider import get_provider

# ---------------------------------------------------------------------------
# Database
# ---------------------------------------------------------------------------

DATABASE_URL = "sqlite:///./ai_studio.db"

engine = create_engine(
    DATABASE_URL, connect_args={"check_same_thread": False}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


class Project(Base):
    __tablename__ = "projects"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)
    description = Column(Text, default="")
    created_at = Column(DateTime, default=datetime.utcnow)

    requirements = relationship("Requirement", back_populates="project")
    tasks = relationship("Task", back_populates="project")
    messages = relationship("Message", back_populates="project")


class Requirement(Base):
    __tablename__ = "requirements"

    id = Column(Integer, primary_key=True, index=True)
    project_id = Column(Integer, ForeignKey("projects.id"), nullable=False)
    title = Column(String(255), nullable=False)
    description = Column(Text, default="")
    improved_description = Column(Text, default="")
    user_stories = Column(Text, default="")
    acceptance_criteria = Column(Text, default="")
    created_at = Column(DateTime, default=datetime.utcnow)

    project = relationship("Project", back_populates="requirements")
    tasks = relationship("Task", back_populates="requirement")


class Task(Base):
    __tablename__ = "tasks"

    id = Column(Integer, primary_key=True, index=True)
    project_id = Column(Integer, ForeignKey("projects.id"), nullable=False)
    requirement_id = Column(Integer, ForeignKey("requirements.id"), nullable=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, default="")
    status = Column(String(50), default="todo")  # todo | in_progress | review | done
    priority = Column(String(50), default="medium")  # low | medium | high
    labels = Column(String(255), default="")
    created_at = Column(DateTime, default=datetime.utcnow)

    project = relationship("Project", back_populates="tasks")
    requirement = relationship("Requirement", back_populates="tasks")


class Message(Base):
    __tablename__ = "messages"

    id = Column(Integer, primary_key=True, index=True)
    project_id = Column(Integer, ForeignKey("projects.id"), nullable=False)
    role = Column(String(50), nullable=False)  # assistant key, e.g. business_analyst
    sender = Column(String(50), nullable=False)  # user | ai
    content = Column(Text, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    project = relationship("Project", back_populates="messages")


# ---------------------------------------------------------------------------
# Assistants — single source of truth
# ---------------------------------------------------------------------------

ASSISTANTS: dict[str, dict[str, str]] = {
    "business_analyst": {
        "name": "Business Analyst",
        "system_prompt": (
            "You are a Business Analyst for a software engineering project. "
            "Clarify requirements, write user stories in As a/I want/So that "
            "form, and produce clear acceptance criteria. Be precise, "
            "structured, and grounded in the project context provided."
        ),
    },
    "developer": {
        "name": "Developer",
        "system_prompt": (
            "You are a Senior Software Developer. Suggest implementation "
            "approaches, break work into technical tasks, call out risks "
            "and trade-offs, and stay aligned with the project's requirements "
            "and current task board."
        ),
    },
    "qa_engineer": {
        "name": "QA Engineer",
        "system_prompt": (
            "You are a QA Engineer. Design test strategies, edge cases, and "
            "acceptance checks from the project's requirements and tasks. "
            "Prefer concrete, verifiable scenarios."
        ),
    },
    "documentation_writer": {
        "name": "Documentation Writer",
        "system_prompt": (
            "You are a Technical Documentation Writer. Produce clear docs, "
            "README sections, and user-facing explanations based on the "
            "project's requirements and tasks. Prefer concise, accurate prose."
        ),
    },
}

VALID_TASK_STATUSES = {"todo", "in_progress", "review", "done"}
VALID_PRIORITIES = {"low", "medium", "high"}

# ---------------------------------------------------------------------------
# Pydantic schemas
# ---------------------------------------------------------------------------


class ProjectCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=255)
    description: str = ""


class ProjectOut(BaseModel):
    id: int
    name: str
    description: Optional[str] = ""
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class RequirementCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=255)
    description: str = ""


class RequirementOut(BaseModel):
    id: int
    project_id: int
    title: str
    description: Optional[str] = ""
    improved_description: Optional[str] = ""
    user_stories: Optional[str] = ""
    acceptance_criteria: Optional[str] = ""
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class TaskCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=255)
    description: str = ""
    priority: str = "medium"
    requirement_id: Optional[int] = None
    labels: str = ""
    status: str = "todo"


class TaskUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    status: Optional[str] = None
    priority: Optional[str] = None
    labels: Optional[str] = None
    requirement_id: Optional[int] = None


class TaskOut(BaseModel):
    id: int
    project_id: int
    requirement_id: Optional[int] = None
    title: str
    description: Optional[str] = ""
    status: str
    priority: str
    labels: Optional[str] = ""
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class ChatMessageIn(BaseModel):
    message: str = Field(..., min_length=1)


class MessageOut(BaseModel):
    id: int
    project_id: int
    role: str
    sender: str
    content: str
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class AssistantOut(BaseModel):
    id: str
    name: str


class ChatReplyOut(BaseModel):
    reply: str
    user_message: MessageOut
    ai_message: MessageOut


# ---------------------------------------------------------------------------
# App + shared helpers
# ---------------------------------------------------------------------------

app = FastAPI(
    title="AI Studio for Software Engineering",
    description="Prototype: shared project context feeding role-specific AI assistants.",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

provider = get_provider()


@app.on_event("startup")
def on_startup() -> None:
    Base.metadata.create_all(bind=engine)


def get_db() -> Session:
    return SessionLocal()


def build_project_context(db: Session, project_id: int) -> str:
    """Assemble requirements + tasks into one text block (the shared context)."""
    project = db.query(Project).filter(Project.id == project_id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")

    requirements = (
        db.query(Requirement)
        .filter(Requirement.project_id == project_id)
        .order_by(Requirement.id.asc())
        .limit(20)
        .all()
    )
    tasks = (
        db.query(Task)
        .filter(Task.project_id == project_id)
        .order_by(Task.id.asc())
        .limit(30)
        .all()
    )

    lines = [
        f"Project: {project.name}",
        f"Description: {project.description or '(none)'}",
        "",
        "=== Requirements ===",
    ]
    if not requirements:
        lines.append("(none)")
    else:
        for req in requirements:
            lines.append(f"- [{req.id}] {req.title}")
            lines.append(f"  Description: {req.description or '(empty)'}")
            if req.improved_description:
                lines.append(f"  Improved: {req.improved_description}")
            if req.user_stories:
                lines.append(f"  User stories: {req.user_stories}")
            if req.acceptance_criteria:
                lines.append(f"  Acceptance criteria: {req.acceptance_criteria}")

    lines.append("")
    lines.append("=== Tasks ===")
    if not tasks:
        lines.append("(none)")
    else:
        for task in tasks:
            req_ref = (
                f", requirement_id={task.requirement_id}"
                if task.requirement_id
                else ""
            )
            lines.append(
                f"- [{task.id}] {task.title} "
                f"(status={task.status}, priority={task.priority}{req_ref})"
            )
            if task.description:
                lines.append(f"  {task.description}")

    return "\n".join(lines)


def ask_assistant(
    db: Session, project_id: int, role: str, user_prompt: str
) -> str:
    """Look up system prompt, append project context, call the provider."""
    if role not in ASSISTANTS:
        raise HTTPException(status_code=400, detail=f"Unknown assistant role: {role}")

    context = build_project_context(db, project_id)
    system = (
        f"{ASSISTANTS[role]['system_prompt']}\n\n"
        f"--- Project Context ---\n{context}\n--- End Context ---"
    )
    return provider.generate(system, user_prompt)


def _get_project_or_404(db: Session, project_id: int) -> Project:
    project = db.query(Project).filter(Project.id == project_id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")
    return project


def _get_requirement_or_404(db: Session, requirement_id: int) -> Requirement:
    req = db.query(Requirement).filter(Requirement.id == requirement_id).first()
    if not req:
        raise HTTPException(status_code=404, detail="Requirement not found")
    return req


def _get_task_or_404(db: Session, task_id: int) -> Task:
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task


# ---------------------------------------------------------------------------
# Projects
# ---------------------------------------------------------------------------


@app.post("/projects", response_model=ProjectOut)
def create_project(body: ProjectCreate):
    db = get_db()
    try:
        project = Project(name=body.name.strip(), description=body.description or "")
        db.add(project)
        db.commit()
        db.refresh(project)
        return project
    finally:
        db.close()


@app.get("/projects", response_model=list[ProjectOut])
def list_projects():
    db = get_db()
    try:
        return db.query(Project).order_by(Project.id.asc()).all()
    finally:
        db.close()


@app.get("/projects/{project_id}", response_model=ProjectOut)
def get_project(project_id: int):
    db = get_db()
    try:
        return _get_project_or_404(db, project_id)
    finally:
        db.close()


# ---------------------------------------------------------------------------
# Requirements
# ---------------------------------------------------------------------------


@app.post("/projects/{project_id}/requirements", response_model=RequirementOut)
def create_requirement(project_id: int, body: RequirementCreate):
    db = get_db()
    try:
        _get_project_or_404(db, project_id)
        req = Requirement(
            project_id=project_id,
            title=body.title.strip(),
            description=body.description or "",
        )
        db.add(req)
        db.commit()
        db.refresh(req)
        return req
    finally:
        db.close()


@app.get("/projects/{project_id}/requirements", response_model=list[RequirementOut])
def list_requirements(project_id: int):
    db = get_db()
    try:
        _get_project_or_404(db, project_id)
        return (
            db.query(Requirement)
            .filter(Requirement.project_id == project_id)
            .order_by(Requirement.id.asc())
            .all()
        )
    finally:
        db.close()


@app.post("/requirements/{requirement_id}/ai/improve", response_model=RequirementOut)
def ai_improve_requirement(requirement_id: int):
    db = get_db()
    try:
        req = _get_requirement_or_404(db, requirement_id)
        prompt = (
            "Improve the following requirement description. Make it clearer, "
            "more complete, and implementable. Return only the improved "
            f"description text.\n\nTitle: {req.title}\nDescription:\n{req.description}"
        )
        result = ask_assistant(db, req.project_id, "business_analyst", prompt)
        req.improved_description = result
        db.commit()
        db.refresh(req)
        return req
    finally:
        db.close()


@app.post(
    "/requirements/{requirement_id}/ai/user-stories", response_model=RequirementOut
)
def ai_user_stories(requirement_id: int):
    db = get_db()
    try:
        req = _get_requirement_or_404(db, requirement_id)
        prompt = (
            "Generate user stories in As a / I want / So that form for this "
            "requirement. Return only the user stories.\n\n"
            f"Title: {req.title}\nDescription:\n{req.description}"
        )
        result = ask_assistant(db, req.project_id, "business_analyst", prompt)
        req.user_stories = result
        db.commit()
        db.refresh(req)
        return req
    finally:
        db.close()


@app.post(
    "/requirements/{requirement_id}/ai/acceptance-criteria",
    response_model=RequirementOut,
)
def ai_acceptance_criteria(requirement_id: int):
    db = get_db()
    try:
        req = _get_requirement_or_404(db, requirement_id)
        prompt = (
            "Generate acceptance criteria for this requirement. Use Given/"
            "When/Then or clear bullet criteria. Return only the acceptance "
            f"criteria.\n\nTitle: {req.title}\nDescription:\n{req.description}"
        )
        result = ask_assistant(db, req.project_id, "business_analyst", prompt)
        req.acceptance_criteria = result
        db.commit()
        db.refresh(req)
        return req
    finally:
        db.close()


# ---------------------------------------------------------------------------
# Tasks
# ---------------------------------------------------------------------------


@app.post("/projects/{project_id}/tasks", response_model=TaskOut)
def create_task(project_id: int, body: TaskCreate):
    db = get_db()
    try:
        _get_project_or_404(db, project_id)
        status = (body.status or "todo").lower()
        priority = (body.priority or "medium").lower()
        if status not in VALID_TASK_STATUSES:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid status. Must be one of: {sorted(VALID_TASK_STATUSES)}",
            )
        if priority not in VALID_PRIORITIES:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid priority. Must be one of: {sorted(VALID_PRIORITIES)}",
            )
        if body.requirement_id is not None:
            req = _get_requirement_or_404(db, body.requirement_id)
            if req.project_id != project_id:
                raise HTTPException(
                    status_code=400,
                    detail="requirement_id does not belong to this project",
                )

        task = Task(
            project_id=project_id,
            requirement_id=body.requirement_id,
            title=body.title.strip(),
            description=body.description or "",
            status=status,
            priority=priority,
            labels=body.labels or "",
        )
        db.add(task)
        db.commit()
        db.refresh(task)
        return task
    finally:
        db.close()


@app.get("/projects/{project_id}/tasks", response_model=list[TaskOut])
def list_tasks(project_id: int):
    db = get_db()
    try:
        _get_project_or_404(db, project_id)
        return (
            db.query(Task)
            .filter(Task.project_id == project_id)
            .order_by(Task.id.asc())
            .all()
        )
    finally:
        db.close()


@app.patch("/tasks/{task_id}", response_model=TaskOut)
def update_task(task_id: int, body: TaskUpdate):
    db = get_db()
    try:
        task = _get_task_or_404(db, task_id)
        data = body.model_dump(exclude_unset=True)

        if "status" in data and data["status"] is not None:
            status = data["status"].lower()
            if status not in VALID_TASK_STATUSES:
                raise HTTPException(
                    status_code=400,
                    detail=f"Invalid status. Must be one of: {sorted(VALID_TASK_STATUSES)}",
                )
            data["status"] = status

        if "priority" in data and data["priority"] is not None:
            priority = data["priority"].lower()
            if priority not in VALID_PRIORITIES:
                raise HTTPException(
                    status_code=400,
                    detail=f"Invalid priority. Must be one of: {sorted(VALID_PRIORITIES)}",
                )
            data["priority"] = priority

        if "requirement_id" in data and data["requirement_id"] is not None:
            req = _get_requirement_or_404(db, data["requirement_id"])
            if req.project_id != task.project_id:
                raise HTTPException(
                    status_code=400,
                    detail="requirement_id does not belong to this project",
                )

        if "title" in data and data["title"] is not None:
            data["title"] = data["title"].strip()
            if not data["title"]:
                raise HTTPException(status_code=400, detail="title cannot be empty")

        for key, value in data.items():
            setattr(task, key, value)

        db.commit()
        db.refresh(task)
        return task
    finally:
        db.close()


# ---------------------------------------------------------------------------
# Assistants + Chat
# ---------------------------------------------------------------------------


@app.get("/assistants", response_model=list[AssistantOut])
def list_assistants():
    return [
        AssistantOut(id=key, name=meta["name"]) for key, meta in ASSISTANTS.items()
    ]


@app.get("/projects/{project_id}/chat/{role}", response_model=list[MessageOut])
def get_chat_history(project_id: int, role: str):
    db = get_db()
    try:
        _get_project_or_404(db, project_id)
        if role not in ASSISTANTS:
            raise HTTPException(status_code=400, detail=f"Unknown assistant role: {role}")
        return (
            db.query(Message)
            .filter(Message.project_id == project_id, Message.role == role)
            .order_by(Message.created_at.asc(), Message.id.asc())
            .all()
        )
    finally:
        db.close()


@app.post("/projects/{project_id}/chat/{role}", response_model=ChatReplyOut)
def send_chat_message(project_id: int, role: str, body: ChatMessageIn):
    db = get_db()
    try:
        _get_project_or_404(db, project_id)
        if role not in ASSISTANTS:
            raise HTTPException(status_code=400, detail=f"Unknown assistant role: {role}")

        user_msg = Message(
            project_id=project_id,
            role=role,
            sender="user",
            content=body.message,
        )
        db.add(user_msg)
        db.commit()
        db.refresh(user_msg)

        reply_text = ask_assistant(db, project_id, role, body.message)

        ai_msg = Message(
            project_id=project_id,
            role=role,
            sender="ai",
            content=reply_text,
        )
        db.add(ai_msg)
        db.commit()
        db.refresh(ai_msg)

        return ChatReplyOut(
            reply=reply_text,
            user_message=user_msg,
            ai_message=ai_msg,
        )
    finally:
        db.close()


@app.get("/health")
def health():
    return {
        "status": "ok",
        "provider": type(provider).__name__,
        "assistants": list(ASSISTANTS.keys()),
    }
