package com.aistudio.application.codemetadata;

import com.aistudio.api.codemetadata.dto.GitConnectionCheckResponse;
import com.aistudio.api.codemetadata.dto.GitConnectionTestResponse;
import com.aistudio.domain.common.DomainException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GitConnectionProbeService {

    public GitConnectionTestResponse probe(GitMetadataPort port, String repository, String branch) {
        List<GitConnectionCheckResponse> checks = new ArrayList<>();
        try {
            port.probeCredential();
            checks.add(new GitConnectionCheckResponse("credential", "success", "API token accepted"));
        } catch (DomainException ex) {
            checks.add(new GitConnectionCheckResponse("credential", "failed", ex.getMessage()));
            return new GitConnectionTestResponse(false, "Credential check failed", checks);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "credential check failed" : ex.getMessage();
            checks.add(new GitConnectionCheckResponse("credential", "failed", message));
            return new GitConnectionTestResponse(false, "Credential check failed", checks);
        }
        if (repository != null && !repository.isBlank()) {
            try {
                port.probeRepository(repository, branch);
                checks.add(new GitConnectionCheckResponse("repository", "success", "Repository accessible"));
            } catch (DomainException ex) {
                checks.add(new GitConnectionCheckResponse("repository", "failed", ex.getMessage()));
                return new GitConnectionTestResponse(false, "Repository check failed", checks);
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? "repository check failed" : ex.getMessage();
                checks.add(new GitConnectionCheckResponse("repository", "failed", message));
                return new GitConnectionTestResponse(false, "Repository check failed", checks);
            }
        }
        return new GitConnectionTestResponse(true, "Connection OK", checks);
    }
}
