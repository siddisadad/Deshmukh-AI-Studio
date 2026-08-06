package com.aistudio.application.plugin.spi;

import com.aistudio.domain.plugin.PluginType;

/**
 * Base SPI for AI Studio extensions (assistants, tools).
 */
public interface StudioPlugin {

    String id();

    String name();

    String version();

    String description();

    PluginType type();

    boolean builtin();
}
