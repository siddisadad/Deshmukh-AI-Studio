package com.aistudio.application.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ThreadExportDlpCustomPattern(String category, String pattern, String description) {
}
