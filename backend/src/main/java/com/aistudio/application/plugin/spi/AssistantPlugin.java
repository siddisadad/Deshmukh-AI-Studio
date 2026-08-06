package com.aistudio.application.plugin.spi;

import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.domain.plugin.PluginType;
import java.util.List;

public interface AssistantPlugin extends StudioPlugin {

    AssistantRole role();

    String promptKey();

    List<String> capabilities();

    List<String> limitations();

    /** Tool plugin ids this assistant can use. */
    List<String> toolIds();

    @Override
    default PluginType type() {
        return PluginType.ASSISTANT;
    }
}
