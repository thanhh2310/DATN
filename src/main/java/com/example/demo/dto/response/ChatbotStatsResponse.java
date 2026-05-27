package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotStatsResponse {
    private long totalSessions;
    private long activeSessions;
    private long closedSessions;
    private long guestSessions;
    private long authenticatedSessions;
    private long todaySessions;
    private long totalMessages;
    private long userMessages;
    private long botMessages;
    private long systemMessages;
    private long todayMessages;
}
