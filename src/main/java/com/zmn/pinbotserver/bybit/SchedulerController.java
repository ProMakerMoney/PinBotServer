package com.zmn.pinbotserver.bybit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final ScheduledTaskService scheduledTaskService;

    @Autowired
    public SchedulerController(ScheduledTaskService scheduledTaskService) {
        this.scheduledTaskService = scheduledTaskService;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startScheduler() {
        scheduledTaskService.startTask();
        return ResponseEntity.ok("Запланированная задача запущена.");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopScheduler() {
        scheduledTaskService.stopTask();
        return ResponseEntity.ok("Запланированная задача остановлена.");
    }
}
