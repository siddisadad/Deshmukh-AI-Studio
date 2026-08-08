package com.aistudio.application.ai;

import com.aistudio.api.ai.dto.ChatMessageResponse;
import com.aistudio.api.ai.dto.ConversationResponse;
import com.aistudio.api.ai.dto.ConversationShareResponse;
import com.aistudio.api.ai.dto.ConversationSummaryResponse;
import com.aistudio.api.ai.dto.CreateConversationRequest;
import com.aistudio.api.ai.dto.ExportedConversation;
import com.aistudio.api.ai.dto.SharedConversationResponse;
import com.aistudio.api.ai.dto.UpdateConversationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.aistudio.application.billing.BillingService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.domain.ai.ConversationVisibility;
import com.aistudio.domain.ai.MessageSender;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.ConversationEntity;
import com.aistudio.infrastructure.persistence.entity.MessageEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.repository.ConversationRepository;
import com.aistudio.infrastructure.persistence.repository.MessageRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.aistudio.shared.util.TokenHashUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
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
    private static final Set<String> EXPORT_FORMATS = Set.of("json", "markdown");

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationService authorizationService;
    private final AssistantRegistry assistantRegistry;
    private final ContextBuilder contextBuilder;
    private final PromptTemplateManager promptTemplateManager;
    private final AiProviderPort aiProviderPort;
    private final BillingService billingService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper exportObjectMapper;
    private final int maxMessages;
    private final long shareTtlSeconds;
    private final String appBaseUrl;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProjectRepository projectRepository,
            ProjectAuthorizationService authorizationService,
            AssistantRegistry assistantRegistry,
            ContextBuilder contextBuilder,
            PromptTemplateManager promptTemplateManager,
            AiProviderPort aiProviderPort,
            BillingService billingService,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper,
            @Value("${aistudio.ai.context.max-messages:20}") int maxMessages,
            @Value("${aistudio.ai.conversation.share-ttl-seconds:604800}") long shareTtlSeconds,
            @Value("${aistudio.billing.app-base-url:http://localhost:5173}") String appBaseUrl
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.assistantRegistry = assistantRegistry;
        this.contextBuilder = contextBuilder;
        this.promptTemplateManager = promptTemplateManager;
        this.aiProviderPort = aiProviderPort;
        this.billingService = billingService;
        this.transactionTemplate = transactionTemplate;
        this.exportObjectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        applyRetentionFromProject(created, project);
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
        if (request.legalHold() != null) {
            conversation.setLegalHold(request.legalHold());
        }
        ProjectEntity project = projectRepository.findById(conversation.getProjectId())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        applyRetentionFromProject(conversation, project);
        conversationRepository.save(conversation);
        return toSummary(conversation);
    }

    @Transactional
    public RetentionPurgeResult purgeExpiredConversations(
            UUID projectId,
            UUID userId,
            boolean complianceExport
    ) {
        authorizationService.requireProjectEdit(projectId, userId);
        Instant now = Instant.now();
        List<ConversationEntity> expired = conversationRepository.findExpiredForRetention(projectId, now);
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        byte[] complianceArchive = null;
        String complianceFilename = null;
        if (complianceExport && !expired.isEmpty()) {
            complianceArchive = buildCompliancePurgeArchive(project, expired, now);
            complianceFilename = safeCompliancePurgeFilename(project, now);
        }
        for (ConversationEntity conversation : expired) {
            conversationRepository.delete(conversation);
        }
        return new RetentionPurgeResult(
                expired.size(),
                complianceExport ? expired.size() : 0,
                complianceFilename,
                complianceArchive
        );
    }

    public record RetentionPurgeResult(
            int purgedCount,
            int exportedCount,
            String complianceArchiveFilename,
            byte[] complianceArchiveBody
    ) {
    }

    @Transactional
    public void reapplyProjectRetentionPolicy(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        List<ConversationEntity> conversations = conversationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        for (ConversationEntity conversation : conversations) {
            applyRetentionFromProject(conversation, project);
            conversationRepository.save(conversation);
        }
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        ConversationEntity conversation = requireConversationEdit(conversationId, userId);
        if (conversation.isLegalHold()) {
            throw new DomainException("LEGAL_HOLD", "Conversation is on legal hold and cannot be deleted");
        }
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
    public ExportedConversation exportConversation(UUID conversationId, UUID userId, String format) {
        ConversationEntity conversation = requireConversationAccess(conversationId, userId);
        List<MessageEntity> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        String normalizedFormat = normalizeExportFormat(format);
        if ("json".equals(normalizedFormat)) {
            return exportAsJson(conversation, messages);
        }
        return exportAsMarkdown(conversation, messages);
    }

    @Transactional(readOnly = true)
    public ExportedConversation exportProjectConversations(
            UUID projectId,
            String roleValue,
            String format,
            UUID userId
    ) {
        authorizationService.requireProjectAccess(projectId, userId);
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        List<ConversationEntity> conversations;
        if (roleValue == null || roleValue.isBlank()) {
            conversations = conversationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        } else {
            conversations = conversationRepository.findByProjectIdAndAssistantRoleOrderByUpdatedAtDesc(
                    projectId, parseRole(roleValue));
        }
        List<ConversationEntity> visible = conversations.stream()
                .filter(c -> canViewConversation(c, userId))
                .toList();
        String normalizedFormat = normalizeExportFormat(format);
        if ("json".equals(normalizedFormat)) {
            return exportProjectAsJson(project, visible);
        }
        return exportProjectAsMarkdown(project, visible);
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
            int[] streamStats = new int[2]; // [chars, deltaCount]
            AiProviderPort.AiGenerationResult result = aiProviderPort.stream(prepared.request(), delta -> {
                if (clientDisconnected.get()) {
                    return;
                }
                streamStats[0] += delta.length();
                streamStats[1] += 1;
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

            Map<String, Object> done = new LinkedHashMap<>();
            done.put("assistantMessage", toChatDto(assistantMessage));
            done.put("provider", aiProviderPort.providerId());
            done.put("model", result.model());
            done.put("usage", streamUsageMap(result, streamStats[0], streamStats[1]));
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

    private Map<String, Object> streamUsageMap(
            AiProviderPort.AiGenerationResult result,
            int streamChars,
            int deltaCount
    ) {
        Map<String, Object> usage = new LinkedHashMap<>();
        if (result.inputTokens() != null) {
            usage.put("inputTokens", result.inputTokens());
        }
        if (result.outputTokens() != null) {
            usage.put("outputTokens", result.outputTokens());
        }
        usage.put("streamChars", streamChars);
        usage.put("deltaCount", deltaCount);
        return usage;
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
        ProjectEntity project = projectRepository.findById(conversation.getProjectId())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        applyRetentionFromProject(conversation, project);
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
                entity.getVisibility().name(),
                entity.isLegalHold(),
                entity.getRetentionExpiresAt()
        );
    }

    private void applyRetentionFromProject(ConversationEntity conversation, ProjectEntity project) {
        Integer days = project.getChatRetentionDays();
        if (days == null || days <= 0) {
            conversation.setRetentionExpiresAt(null);
            return;
        }
        Instant anchor = conversation.getUpdatedAt() != null ? conversation.getUpdatedAt() : conversation.getCreatedAt();
        if (anchor == null) {
            anchor = Instant.now();
        }
        conversation.setRetentionExpiresAt(anchor.plus(days, ChronoUnit.DAYS));
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

    private String normalizeExportFormat(String format) {
        String normalized = format == null || format.isBlank() ? "markdown" : format.trim().toLowerCase();
        if (!EXPORT_FORMATS.contains(normalized)) {
            throw new DomainException("VALIDATION_ERROR", "format must be json or markdown");
        }
        return normalized;
    }

    private ExportedConversation exportAsJson(ConversationEntity conversation, List<MessageEntity> messages) {
        try {
            byte[] body = exportObjectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(conversationExportMap(conversation, messages));
            return new ExportedConversation(
                    body,
                    MediaType.APPLICATION_JSON_VALUE,
                    safeExportFilename(conversation, "json"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize conversation export", ex);
        }
    }

    private ExportedConversation exportAsMarkdown(ConversationEntity conversation, List<MessageEntity> messages) {
        byte[] body = buildConversationMarkdown(conversation, messages).getBytes(StandardCharsets.UTF_8);
        return new ExportedConversation(body, "text/markdown; charset=UTF-8", safeExportFilename(conversation, "md"));
    }

    private ExportedConversation exportProjectAsJson(ProjectEntity project, List<ConversationEntity> conversations) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("projectKey", project.getProjectKey());
        payload.put("projectName", project.getName());
        payload.put("exportedAt", Instant.now());
        payload.put("conversationCount", conversations.size());
        List<Map<String, Object>> conversationPayloads = new ArrayList<>();
        for (ConversationEntity conversation : conversations) {
            List<MessageEntity> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            conversationPayloads.add(conversationExportMap(conversation, messages));
        }
        payload.put("conversations", conversationPayloads);
        try {
            byte[] body = exportObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
            return new ExportedConversation(
                    body,
                    MediaType.APPLICATION_JSON_VALUE,
                    safeProjectExportFilename(project, "json"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize project conversation archive", ex);
        }
    }

    private ExportedConversation exportProjectAsMarkdown(ProjectEntity project, List<ConversationEntity> conversations) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Project archive: ").append(project.getName()).append("\n\n");
        markdown.append("- **Project key:** ").append(project.getProjectKey()).append("\n");
        markdown.append("- **Exported:** ").append(Instant.now()).append("\n");
        markdown.append("- **Threads:** ").append(conversations.size()).append("\n\n");
        markdown.append("---\n\n");
        for (ConversationEntity conversation : conversations) {
            List<MessageEntity> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            markdown.append(buildConversationMarkdown(conversation, messages)).append("\n\n---\n\n");
        }
        byte[] body = markdown.toString().getBytes(StandardCharsets.UTF_8);
        return new ExportedConversation(
                body,
                "text/markdown; charset=UTF-8",
                safeProjectExportFilename(project, "md"));
    }

    private Map<String, Object> conversationExportMap(ConversationEntity conversation, List<MessageEntity> messages) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", conversation.getId());
        map.put("projectId", conversation.getProjectId());
        map.put("assistantRole", conversation.getAssistantRole().name());
        map.put("title", conversation.getTitle());
        map.put("visibility", conversation.getVisibility().name());
        map.put("exportedAt", Instant.now());
        map.put("messages", messages.stream()
                .map(m -> Map.of(
                        "id", m.getId(),
                        "sender", m.getSender().name(),
                        "content", m.getContent(),
                        "createdAt", m.getCreatedAt()))
                .toList());
        return map;
    }

    private Map<String, Object> complianceConversationExportMap(
            ConversationEntity conversation,
            List<MessageEntity> messages,
            Instant purgedAt
    ) {
        Map<String, Object> map = conversationExportMap(conversation, messages);
        map.put("legalHold", conversation.isLegalHold());
        map.put("retentionExpiresAt", conversation.getRetentionExpiresAt());
        map.put("createdAt", conversation.getCreatedAt());
        map.put("updatedAt", conversation.getUpdatedAt());
        map.put("purgeReason", "retention_expired");
        map.put("purgedAt", purgedAt);
        return map;
    }

    private byte[] buildCompliancePurgeArchive(
            ProjectEntity project,
            List<ConversationEntity> expired,
            Instant purgedAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("projectKey", project.getProjectKey());
        payload.put("projectName", project.getName());
        payload.put("purgedAt", purgedAt);
        payload.put("purgeReason", "retention_expired");
        payload.put("purgedCount", expired.size());
        List<Map<String, Object>> conversations = new ArrayList<>();
        for (ConversationEntity conversation : expired) {
            List<MessageEntity> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            conversations.add(complianceConversationExportMap(conversation, messages, purgedAt));
        }
        payload.put("conversations", conversations);
        try {
            byte[] json = exportObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
            ByteArrayOutputStream gzip = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(gzip)) {
                gzos.write(json);
            }
            return gzip.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build compliance purge archive", ex);
        }
    }

    private String safeCompliancePurgeFilename(ProjectEntity project, Instant purgedAt) {
        String base = project.getProjectKey() == null ? "project" : project.getProjectKey().trim();
        base = base.replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("-+", "-");
        if (base.isBlank() || base.equals("-")) {
            base = "project";
        }
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        return base + "-compliance-purge-" + purgedAt.toString().substring(0, 10) + ".json.gz";
    }

    private String buildConversationMarkdown(ConversationEntity conversation, List<MessageEntity> messages) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(conversation.getTitle() == null ? "Conversation" : conversation.getTitle())
                .append("\n\n");
        markdown.append("- **Assistant:** ").append(conversation.getAssistantRole().name()).append("\n");
        markdown.append("- **Visibility:** ").append(conversation.getVisibility().name()).append("\n");
        markdown.append("- **Exported:** ").append(Instant.now()).append("\n\n");
        markdown.append("---\n\n");
        for (MessageEntity message : messages) {
            String label = message.getSender() == MessageSender.USER ? "User" : "Assistant";
            markdown.append("### ").append(label).append(" — ").append(message.getCreatedAt()).append("\n\n");
            markdown.append(message.getContent()).append("\n\n");
        }
        return markdown.toString();
    }

    private String safeExportFilename(ConversationEntity conversation, String extension) {
        String base = conversation.getTitle() == null ? "conversation" : conversation.getTitle().trim();
        base = base.replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("-+", "-");
        if (base.isBlank() || base.equals("-")) {
            base = "conversation";
        }
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        return base + "-" + conversation.getId().toString().substring(0, 8) + "." + extension;
    }

    private String safeProjectExportFilename(ProjectEntity project, String extension) {
        String base = project.getProjectKey() == null ? "project" : project.getProjectKey().trim();
        base = base.replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("-+", "-");
        if (base.isBlank() || base.equals("-")) {
            base = "project";
        }
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        return base + "-threads-" + project.getId().toString().substring(0, 8) + "." + extension;
    }

    private record PreparedChat(
            ConversationEntity conversation,
            MessageEntity userMessage,
            AiProviderPort.AiGenerationRequest request,
            String promptVersion
    ) {
    }
}
