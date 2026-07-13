package com.joao.storemanagement.dto.dashboard;

import java.util.Map;

public record DashboardQuickLinkDTO(String label, String route, Map<String, String> query) {}
