package com.digitaltimeline.web;

import com.digitaltimeline.dto.DashboardDtos.Dashboard;
import com.digitaltimeline.dto.DashboardDtos.Notice;
import com.digitaltimeline.service.DashboardService;
import com.digitaltimeline.service.NotificationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboard;
    private final NotificationService notifications;

    public DashboardController(DashboardService dashboard, NotificationService notifications) {
        this.dashboard = dashboard;
        this.notifications = notifications;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String tz) {
        return dashboard.build(CurrentUser.id(jwt), CurrentUser.zone(tz));
    }

    @GetMapping("/notifications")
    public List<Notice> notifications(@AuthenticationPrincipal Jwt jwt) {
        return notifications.notices(CurrentUser.id(jwt));
    }
}
