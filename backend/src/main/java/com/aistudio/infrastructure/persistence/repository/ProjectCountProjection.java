package com.aistudio.infrastructure.persistence.repository;

import java.util.UUID;

public interface ProjectCountProjection {
    UUID getProjectId();

    long getCount();
}
