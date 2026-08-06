package com.aistudio.application.knowledge;

import java.util.List;

public interface EmbeddingPort {
    float[] embed(String text);

    List<float[]> embedAll(List<String> texts);

    String providerId();

    int dimensions();
}
