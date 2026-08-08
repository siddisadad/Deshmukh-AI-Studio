package com.aistudio.application.ai;

import com.aistudio.api.ai.dto.ChatMessageResponse;
import com.aistudio.api.ai.dto.ConversationResponse;
import com.aistudio.api.ai.dto.ConversationShareResponse;
import com.aistudio.api.ai.dto.ConversationSummaryResponse;
import com.aistudio.api.ai.dto.CreateConversationRequest;
import com.aistudio.api.ai.dto.SharedConversationResponse;
import com.aistudio.api.ai.dto.UpdateConversationRequest;
import com.aistudio.application.billing.BillingService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.domain.ai.ConversationVisibility;
import com.aistudio.domain.ai.MessageSender;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.ConversationEntity;
import com.aistudio.infrastructure.persistence.entity.MessageEntity;
import com.aistudio.infrastructure.persistence.repository.ConversationRepository;
import com.aistudio.infrastructure.persistence.repository.MessageRepository;
import com.aistudio.shared.util.TokenHashUtils;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

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
    private final long shareTtlSeconds;
    private final String appBaseUrl;

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
            @Value("${aistudio.ai.context.max-messages:20}") int maxMessages,
            @Value("${aistudio.ai.conversation.share-ttl-seconds:604800}") long shareTtlSeconds,
            @Value("${aistudio.billing.app-base-url:http://localhost:5173}") String appBaseUrl
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
        this.shareTtlSeconds = shareTtlSeconds;
        this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listConversations(
            UUID projectId,
            String roleValue,
            String query,
            UUID userId
    ) {
        authorizationService.requireProjectAccess(projectId, userId);
        List<ConversationEntity> conversations;
        String trimmedQuery = query == null ? null : query.trim();
        if (trimmedQuery != null && !trimmedQuery.isBlank()) {
            if (roleValue == null || roleValue.isBlank()) {
                conversations = conversationRepository.searchByProjectId(projectId, trimmedQuery);
            } else {
                conversations = conversationRepository.searchByProjectIdAndAssistantRole(
                        projectId, parseRole(roleValue).name(), trimmedQuery);
            }
        } else if (roleValue == null || roleValue.isBlank()) {
            conversations = conversationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        } else {
            conversations = conversationRepository.findByProjectIdAndAssistantRoleOrderByUpdatedAtDesc(
                    projectId, parseRole(roleValue));
        }
        return conversations.stream()
                .filter(c -> canViewConversation(c, userId))
                .map(this::toSummary)
                .toList();
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
        created.setVisibility(parseVisibility(request.visibility()));
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
        if (request.visibility() != null && !request.visibility().isBlank()) {
            conversation.setVisibility(parseVisibility(request.visibility()));
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
    public ConversationShareResponse enableShare(UUID conversationId, UUID userId) {
        ConversationEntity conversation = requireConversationEdit(conversationId, userId);
        String rawToken = TokenHashUtils.generateOpaqueToken();
        Instant now = Instant.now();
        conversation.setShareEnabled(true);
        conversation.setShareTokenHash(TokenHashUtils.sha256(rawToken));
        conversation.setShareCreatedAt(now);
        conversation.setShareExpiresAt(now.plusSeconds(shareTtlSeconds));
        conversationRepository.save(conversation);
        return toShareResponse(conversation, rawToken);
    }

    @Transactional
    public void revokeShare(UUID conversationId, UUID userId) {
        ConversationEntity conversation = requireConversationEdit(conversationId, userId);
        clearShare(conversation);
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public SharedConversationResponse getSharedConversation(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new DomainException("NOT_FOUND", "Shared conversation not found");
        }
        ConversationEntity conversation = conversationRepository
                .findByShareTokenHash(TokenHashUtils.sha256(rawToken.trim()))
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Shared conversation not found"));
        if (!isShareActive(conversation)) {
            throw new DomainException("NOT_FOUND", "Shared conversation not found");
        }
        List<SharedConversationResponse.MessageDto> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(m -> new SharedConversationResponse.MessageDto(
                        m.getId(),
                        m.getSender().name(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .toList();
        return new SharedConversationResponse(
                conversation.getAssistantRole().name(),
                conversation.getTitle(),
                conversation.getShareExpiresAt(),
                messages
        );
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

            AtomicBoolean clientDisconnected = new AtomicBoolean(false);
            AiProviderPort.AiGenerationResult result = aiProviderPort.stream(prepared.request(), delta -> {
                if (clientDisconnected.get()) {
                    return;
                }
                try {
                    emitter.send(SseEmitter.event()
                            .name("delta")
                            .data(Map.of("text", delta), MediaType.APPLICATION_JSON));
                } catch (IOException ex) {
                    clientDisconnected.set(true);
                    log.warn("SSE client disconnected during stream for conversation {}", conversationId);
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
            if (!clientDisconnected.get()) {
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(done, MediaType.APPLICATION_JSON));
                emitter.complete();
            } else {
                emitter.complete();
            }
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
        if (!canViewConversation(conversation, userId)) {
            throw new DomainException("NOT_FOUND", "Conversation not found");
        }
        return conversation;
    }

    private ConversationEntity requireConversationEdit(UUID conversationId, UUID userId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Conversation not found"));
        if (conversation.getVisibility() == ConversationVisibility.PRIVATE) {
            authorizationService.requireProjectAccess(conversation.getProjectId(), userId);
            if (!isCreator(conversation, userId)) {
                throw new DomainException("NOT_FOUND", "Conversation not found");
            }
            return conversation;
        }
        authorizationService.requireProjectEdit(conversation.getProjectId(), userId);
        return conversation;
    }

    private boolean canViewConversation(ConversationEntity conversation, UUID userId) {
        if (conversation.getVisibility() == ConversationVisibility.PROJECT) {
            return true;
        }
        return isCreator(conversation, userId);
    }

    private boolean isCreator(ConversationEntity conversation, UUID userId) {
        return conversation.getCreatedBy() != null && conversation.getCreatedBy().equals(userId);
    }

    private ConversationVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return ConversationVisibility.PROJECT;
        }
        try {
            return ConversationVisibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DomainException("VALIDATION_ERROR", "visibility must be PROJECT or PRIVATE");
        }
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
                (int) messageRepository.countByConversationId(entity.getId()),
                entity.isShareEnabled() && isShareActive(entity),
                entity.getShareExpiresAt(),
                entity.getVisibility().name()
        );
    }

    private ConversationShareResponse toShareResponse(ConversationEntity entity, String rawToken) {
        String encoded = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String shareUrl = appBaseUrl + "/shared/chat/" + encoded;
        return new ConversationShareResponse(
                true,
                shareUrl,
                rawToken,
                entity.getShareExpiresAt()
        );
    }

    private boolean isShareActive(ConversationEntity entity) {
        return entity.isShareEnabled()
                && entity.getShareTokenHash() != null
                && entity.getShareExpiresAt() != null
                && entity.getShareExpiresAt().isAfter(Instant.now());
    }

    private void clearShare(ConversationEntity entity) {
        entity.setShareEnabled(false);
        entity.setShareTokenHash(null);
        entity.setShareExpiresAt(null);
        entity.setShareCreatedAt(null);
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
