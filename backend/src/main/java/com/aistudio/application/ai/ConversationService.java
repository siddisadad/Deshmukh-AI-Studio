package com.aistudio.application.ai;

import com.aistudio.api.ai.dto.ChatMessageResponse;
import com.aistudio.api.ai.dto.ConversationResponse;
import com.aistudio.api.ai.dto.ConversationSummaryResponse;
import com.aistudio.api.ai.dto.CreateConversationRequest;
import com.aistudio.api.ai.dto.UpdateConversationRequest;
import com.aistudio.application.billing.BillingService;
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
    private final BillingService billingService;
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
            BillingService billingService,
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
        this.billingService = billingService;
        this.transactionTemplate = transactionTemplate;
        this.maxMessages = maxMessages;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listConversations(UUID projectId, String roleValue, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        List<ConversationEntity> conversations;
        if (roleValue == null || roleValue.isBlank()) {
            conversations = conversationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        } else {
            conversations = conversationRepository.findByProjectIdAndAssistantRoleOrderByUpdatedAtDesc(
                    projectId, parseRole(roleValue));
        }
        return conversations.stream().map(this::toSummary).toList();
    }

    @Transactional
    public ConversationSummaryResponse createConversation(
            UUID projectId,
            UUID userId,
            CreateConversationRequest request
    ) {
        authorizationService.requireProjectEdit(projectId, userId);
        AssistantRole role = parseRole(request.assistantRole());
        AssistantRegistry.AssistantDefinition assistant = assistantRegistry.require(role);

        ConversationEntity created = new ConversationEntity();
        created.setProjectId(projectId);
        created.setAssistantRole(role);
        created.setCreatedBy(userId);
        String title = request.title() == null || request.title().isBlank()
                ? "New " + assistant.name() + " thread"
                : request.title().trim();
        created.setTitle(title);
        conversationRepository.save(created);
        return toSummary(created);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        ConversationEntity conversation = requireConversationAccess(conversationId, userId);
        List<ConversationResponse.MessageDto> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::toMessageDto)
                .toList();
        return new ConversationResponse(
                conversation.getId(),
                conversation.getProjectId(),
                conversation.getAssistantRole().name(),
                conversation.getTitle(),
                messages
        );
    }

    @Transactional
    public ConversationSummaryResponse updateConversation(
            UUID conversationId,
            UUID userId,
            UpdateConversationRequest request
    ) {
        ConversationEntity conversation = requireConversationEdit(conversationId, userId);
        if (request.title() != null && !request.title().isBlank()) {
            conversation.setTitle(request.title().trim());
        }
        conversationRepository.save(conversation);
        return toSummary(conversation);
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        ConversationEntity conversation = requireConversationEdit(conversationId, userId);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID conversationId, UUID userId, String content) {
        PreparedChat prepared = prepareChat(conversationId, userId, content);
        AiProviderPort.AiGenerationResult result = aiProviderPort.generate(prepared.request());
        MessageEntity assistantMessage = persistAssistant(prepared.conversation(), result, prepared.promptVersion());
        return new ChatMessageResponse(
                toChatDto(prepared.userMessage()),
                toChatDto(assistantMessage),
                aiProviderPort.providerId(),
                result.model()
        );
    }

    public void streamMessage(UUID conversationId, UUID userId, String content, SseEmitter emitter) {
        try {
            PreparedChat prepared = transactionTemplate.execute(status ->
                    prepareChat(conversationId, userId, content));
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
                    persistAssistant(prepared.conversation(), result, prepared.promptVersion()));
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

    private PreparedChat prepareChat(UUID conversationId, UUID userId, String content) {
        ConversationEntity conversation = requireConversationEdit(conversationId, userId);
        billingService.requireAndConsumeAiActionForProject(conversation.getProjectId());
        AssistantRegistry.AssistantDefinition assistant = assistantRegistry.require(conversation.getAssistantRole());

        MessageEntity userMessage = new MessageEntity();
        userMessage.setConversationId(conversation.getId());
        userMessage.setSender(MessageSender.USER);
        userMessage.setContent(content.trim());
        userMessage.setMetadata("{}");
        messageRepository.save(userMessage);

        maybeAutoTitle(conversation, userMessage.getContent());

        String context = contextBuilder.buildForProject(conversation.getProjectId(), content.trim());
        String systemPrompt = promptTemplateManager.systemPrompt(assistant.promptKey())
                + "\n\n## Shared project context\n" + context;
        String promptVersion = promptTemplateManager.systemPromptVersion(assistant.promptKey());

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
                Map.of(
                        "assistantRole", conversation.getAssistantRole().name(),
                        "promptVersion", promptVersion
                )
        );
        return new PreparedChat(conversation, userMessage, request, promptVersion);
    }

    private void maybeAutoTitle(ConversationEntity conversation, String firstUserContent) {
        String title = conversation.getTitle();
        boolean isDefault = title == null
                || title.startsWith("New ")
                || title.endsWith(" chat")
                || title.endsWith(" thread");
        long count = messageRepository.countByConversationId(conversation.getId());
        if (isDefault && count <= 1) {
            String trimmed = firstUserContent.replace('\n', ' ').trim();
            if (trimmed.length() > 60) {
                trimmed = trimmed.substring(0, 57) + "…";
            }
            if (!trimmed.isBlank()) {
                conversation.setTitle(trimmed);
                conversationRepository.save(conversation);
            }
        }
    }

    private MessageEntity persistAssistant(
            ConversationEntity conversation,
            AiProviderPort.AiGenerationResult result,
            String promptVersion
    ) {
        MessageEntity assistantMessage = new MessageEntity();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setSender(MessageSender.ASSISTANT);
        assistantMessage.setContent(result.text());
        assistantMessage.setMetadata("""
                {"provider":"%s","model":"%s","promptVersion":"%s"}
                """.formatted(aiProviderPort.providerId(), result.model(), promptVersion).trim());
        messageRepository.save(assistantMessage);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        return assistantMessage;
    }

    private ConversationEntity requireConversationAccess(UUID conversationId, UUID userId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Conversation not found"));
        authorizationService.requireProjectAccess(conversation.getProjectId(), userId);
        return conversation;
    }

    private ConversationEntity requireConversationEdit(UUID conversationId, UUID userId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Conversation not found"));
        authorizationService.requireProjectEdit(conversation.getProjectId(), userId);
        return conversation;
    }

    private AssistantRole parseRole(String roleValue) {
        try {
            return assistantRegistry.parseRole(roleValue);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("VALIDATION_ERROR", ex.getMessage());
        }
    }

    private ConversationSummaryResponse toSummary(ConversationEntity entity) {
        return new ConversationSummaryResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getAssistantRole().name(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                (int) messageRepository.countByConversationId(entity.getId())
        );
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
            ConversationEntity conversation,
            MessageEntity userMessage,
            AiProviderPort.AiGenerationRequest request,
            String promptVersion
    ) {
    }
}
