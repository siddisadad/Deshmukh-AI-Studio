package com.aistudio.api.billing.dto;

import java.time.LocalDate;

public record UsageDayResponse(LocalDate date, int actionCount) {
}
