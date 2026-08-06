package com.aistudio.application.ai;

import com.aistudio.api.ai.dto.ChatMessageResponse;
import com.aistudio.api.ai.dto.ConversationResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.domain.ai.MessageSender;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.ConversationEntity;
import com.aistudio.infrastructure.persistence.entity.MessageEntity;
import com.aistudio.infrastructure.persistence.repository.ConversationRepository;
import com.aistudio.infrastructure.persistence.repository.MessageRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProjectAuthorizationService authorizationService;
    private final AssistantRegistry assistantRegistry;
    private final ContextBuilder contextBuilder;
    private final PromptTemplateManager promptTemplateManager;
    private final AiProviderPort aiProviderPort;
    private final TransactionTemplate transactionTemplate;
    private final int maxMessages;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProjectAuthorizationService authorizationService,
            AssistantRegistry assistantRegistry,
            ContextBuilder contextBuilder,
            PromptTemplateManager promptTemplateManager,
            AiProviderPort aiProviderPort,
            TransactionTemplate transactionTemplate,
            @Value("${aistudio.ai.context.max-messages:20}") int maxMessages
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.authorizationService = authorizationService;
        this.assistantRegistry = assistantRegistry;
        this.contextBuilder = contextBuilder;
        this.promptTemplateManager = promptTemplateManager;
        this.aiProviderPort = aiProviderPort;
        this.transactionTemplate = transactionTemplate;
        this.maxMessages = maxMessages;
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID projectId, String roleValue, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        AssistantRole role = parseRole(roleValue);
        ConversationEntity conversation = conversationRepository.findByProjectIdAndAssistantRole(projectId, role)
                .orElse(null);
        if (conversation == null) {
            return new ConversationResponse(null, projectId, role.name(), null, List.of());
        }
        List<ConversationResponse.MessageDto> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::toMessageDto)
                .toList();
        return new ConversationResponse(
                conversation.getId(),
                projectId,
                role.name(),
                conversation.getTitle(),
                messages
        );
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID projectId, String roleValue, UUID userId, String content) {
        PreparedChat prepared = prepareChat(projectId, roleValue, userId, content);
        AiProviderPort.AiGenerationResult result = aiProviderPort.generate(prepared.request());
        MessageEntity assistantMessage = persistAssistant(prepared.conversationId(), result);
        return new ChatMessageResponse(
                toChatDto(prepared.userMessage()),
                toChatDto(assistantMessage),
                aiProviderPort.providerId(),
                result.model()
        );
    }

    public void streamMessage(UUID projectId, String roleValue, UUID userId, String content, SseEmitter emitter) {
        try {
            PreparedChat prepared = transactionTemplate.execute(status ->
                    prepareChat(projectId, roleValue, userId, content));
            if (prepared == null) {
                throw new IllegalStateException("Failed to prepare chat");
            }

            emitter.send(SseEmitter.event()
                    .name("user")
                    .data(toChatDto(prepared.userMessage()), MediaType.APPLICATION_JSON));

            AiProviderPort.AiGenerationResult result = aiProviderPort.stream(prepared.request(), delta -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("delta")
                            .data(Map.of("text", delta), MediaType.APPLICATION_JSON));
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to write SSE delta", ex);
                }
            });

            MessageEntity assistantMessage = transactionTemplate.execute(status ->
                    persistAssistant(prepared.conversationId(), result));
            if (assistantMessage == null) {
                throw new IllegalStateException("Failed to persist assistant message");
            }

            Map<String, Object> done = Map.of(
                    "assistantMessage", toChatDto(assistantMessage),
                    "provider", aiProviderPort.providerId(),
                    "model", result.model()
            );
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(done, MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception ex) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of(
                                "message", ex.getMessage() == null ? "Stream failed" : ex.getMessage()
                        ), MediaType.APPLICATION_JSON));
            } catch (Exception ignored) {
                // emitter may already be closed
            }
            emitter.completeWithError(ex);
        }
    }

    private PreparedChat prepareChat(UUID projectId, String roleValue, UUID userId, String content) {
        authorizationService.requireProjectEdit(projectId, userId);
        AssistantRole role = parseRole(roleValue);
        AssistantRegistry.AssistantDefinition assistant = assistantRegistry.require(role);

        ConversationEntity conversation = conversationRepository.findByProjectIdAndAssistantRole(projectId, role)
                .orElseGet(() -> {
                    ConversationEntity created = new ConversationEntity();
                    created.setProjectId(projectId);
                    created.setAssistantRole(role);
                    created.setTitle(assistant.name() + " chat");
                    created.setCreatedBy(userId);
                    return conversationRepository.save(created);
                });

        MessageEntity userMessage = new MessageEntity();
        userMessage.setConversationId(conversation.getId());
        userMessage.setSender(MessageSender.USER);
        userMessage.setContent(content.trim());
        userMessage.setMetadata("{}");
        messageRepository.save(userMessage);

        String context = contextBuilder.buildForProject(projectId);
        String systemPrompt = promptTemplateManager.systemPrompt(assistant.promptKey())
                + "\n\n## Shared project context\n" + context;

        List<MessageEntity> recentDesc = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversation.getId(), PageRequest.of(0, maxMessages));
        List<MessageEntity> recent = new ArrayList<>(recentDesc);
        Collections.reverse(recent);

        List<AiProviderPort.AiMessage> aiMessages = recent.stream()
                .map(m -> new AiProviderPort.AiMessage(
                        m.getSender() == MessageSender.USER ? "user" : "assistant",
                        m.getContent()
                ))
                .toList();

        AiProviderPort.AiGenerationRequest request = new AiProviderPort.AiGenerationRequest(
                systemPrompt,
                aiMessages,
                0.3,
                2000,
                Map.of("assistantRole", role.name())
        );
        return new PreparedChat(conversation.getId(), userMessage, request);
    }

    private MessageEntity persistAssistant(UUID conversationId, AiProviderPort.AiGenerationResult result) {
        MessageEntity assistantMessage = new MessageEntity();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setSender(MessageSender.ASSISTANT);
        assistantMessage.setContent(result.text());
        assistantMessage.setMetadata("""
                {"provider":"%s","model":"%s"}
                """.formatted(aiProviderPort.providerId(), result.model()).trim());
        messageRepository.save(assistantMessage);

        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
        });
        return assistantMessage;
    }

    private AssistantRole parseRole(String roleValue) {
        try {
            return assistantRegistry.parseRole(roleValue);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("VALIDATION_ERROR", ex.getMessage());
        }
    }

    private ConversationResponse.MessageDto toMessageDto(MessageEntity entity) {
        return new ConversationResponse.MessageDto(
                entity.getId(),
                entity.getSender().name(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    private ChatMessageResponse.MessageDto toChatDto(MessageEntity entity) {
        return new ChatMessageResponse.MessageDto(
                entity.getId(),
                entity.getSender().name(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    private record PreparedChat(
            UUID conversationId,
            MessageEntity userMessage,
            AiProviderPort.AiGenerationRequest request
    ) {
    }
}
