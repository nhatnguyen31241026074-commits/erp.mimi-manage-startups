package com.techforge.erp.service;

import com.techforge.erp.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final WorkLogService workLogService;

    @Autowired
    public ReportService(ProjectService projectService, TaskService taskService, WorkLogService workLogService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.workLogService = workLogService;
    }

    public CompletableFuture<ProjectReport> generateProjectReport(String projectId) {
        CompletableFuture<Project> projectF = projectService.getProjectById(projectId);
        CompletableFuture<List<Task>> tasksF = taskService.getAllTasks()
                .thenApply(list -> list.stream().filter(t -> projectId.equals(t.getProjectId())).collect(Collectors.toList()));
        CompletableFuture<List<WorkLog>> worklogsF = workLogService.getAllWorkLogs()
                .thenApply(list -> list.stream().filter(w -> projectId.equals(w.getProjectId())).collect(Collectors.toList()));

        return CompletableFuture.allOf(projectF, tasksF, worklogsF)
                .thenApply(v -> {
                    Project project = projectF.join();
                    if (project == null) throw new IllegalStateException("Project not found: " + projectId);

                    List<Task> tasks = tasksF.join();
                    List<WorkLog> worklogs = worklogsF.join();

                    int totalTasks = tasks.size();
                    int completedTasks = (int) tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus()) || "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
                    double progress = totalTasks == 0 ? 0.0 : (completedTasks * 100.0 / totalTasks);

                    // budgetUsed: estimate from worklogs using snapshots
                    double budgetUsed = 0.0;
                    for (WorkLog w : worklogs) {
                        double base = w.getBaseSalarySnapshot() == null ? 0.0 : w.getBaseSalarySnapshot();
                        double hourly = w.getHourlyRateOTSnapshot() == null ? 0.0 : w.getHourlyRateOTSnapshot();
                        double regRate = base > 0 ? base / 160.0 : 0.0; // assumption
                        double reg = w.getRegularHours() == null ? 0.0 : w.getRegularHours();
                        double ot = w.getOvertimeHours() == null ? 0.0 : w.getOvertimeHours();
                        budgetUsed += reg * regRate + ot * hourly;
                    }
                    double projectBudget = project.getBudget() == null ? 0.0 : project.getBudget();
                    double budgetRemaining = projectBudget - budgetUsed;

                    Map<String, Long> breakdown = tasks.stream().collect(Collectors.groupingBy(t -> t.getStatus() == null ? "UNKNOWN" : t.getStatus(), Collectors.counting()));
                    List<Map<String, Object>> taskBreakdown = breakdown.entrySet().stream().map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("status", e.getKey());
                        m.put("count", e.getValue());
                        return m;
                    }).collect(Collectors.toList());

                    Map<String, Double> contribution = new HashMap<>();
                    for (WorkLog w : worklogs) {
                        String userId = w.getUserId() == null ? "unknown" : w.getUserId();
                        double hrs = w.getHours() == null ? 0.0 : w.getHours();
                        contribution.put(userId, contribution.getOrDefault(userId, 0.0) + hrs);
                    }
                    List<Map<String, Object>> workerContribution = contribution.entrySet().stream().map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("userId", e.getKey());
                        m.put("hours", e.getValue());
                        return m;
                    }).collect(Collectors.toList());

                    ProjectReport report = new ProjectReport();
                    report.setProjectId(project.getId());
                    report.setProjectName(project.getName());
                    report.setProgress(round(progress));
                    report.setBudgetUsed(round(budgetUsed));
                    report.setBudgetRemaining(round(budgetRemaining));
                    report.setTotalTasks(totalTasks);
                    report.setCompletedTasks(completedTasks);
                    report.setTaskBreakdown(taskBreakdown);
                    report.setWorkerContribution(workerContribution);

                    return report;
                });
    }

    public CompletableFuture<MonthlyReport> generateMonthlyReport(int month, int year) {
        CompletableFuture<List<Project>> projectsF = projectService.getAllProjects();
        CompletableFuture<List<WorkLog>> worklogsF = workLogService.getAllWorkLogs();

        return CompletableFuture.allOf(projectsF, worklogsF).thenApply(v -> {
            List<Project> projects = projectsF.join();
            List<WorkLog> allWorklogs = worklogsF.join();

            List<WorkLog> filtered = allWorklogs.stream().filter(w -> {
                if (w.getWorkDate() == null) return false;
                LocalDate ld = Instant.ofEpochMilli(w.getWorkDate().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
                return ld.getMonthValue() == month && ld.getYear() == year;
            }).collect(Collectors.toList());

            double totalPayroll = 0.0;
            for (WorkLog w : filtered) {
                double base = w.getBaseSalarySnapshot() == null ? 0.0 : w.getBaseSalarySnapshot();
                double hourlyOT = w.getHourlyRateOTSnapshot() == null ? 0.0 : w.getHourlyRateOTSnapshot();
                double regRate = base > 0 ? base / 160.0 : 0.0;
                double reg = w.getRegularHours() == null ? 0.0 : w.getRegularHours();
                double ot = w.getOvertimeHours() == null ? 0.0 : w.getOvertimeHours();
                totalPayroll += reg * regRate + ot * hourlyOT;
            }

            double totalRevenue = 0.0; // requires invoices integration
            double totalExpense = 0.0; // requires expense integration
            double profit = totalRevenue - totalExpense - totalPayroll;

            MonthlyReport report = new MonthlyReport();
            report.setMonth(month);
            report.setYear(year);
            report.setTotalRevenue(round(totalRevenue));
            report.setTotalExpense(round(totalExpense));
            report.setTotalPayroll(round(totalPayroll));
            report.setProfit(round(profit));

            List<String> projectIds = filtered.stream().map(WorkLog::getProjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            report.setProjects(projectIds);
            report.setPayrolls(Collections.emptyList());

            return report;
        });
    }

    public CompletableFuture<ProgressReport> getProjectProgress(String projectId) {
        CompletableFuture<List<Task>> tasksF = taskService.getAllTasks().thenApply(list -> list.stream().filter(t -> projectId.equals(t.getProjectId())).collect(Collectors.toList()));
        CompletableFuture<List<WorkLog>> worklogsF = workLogService.getAllWorkLogs().thenApply(list -> list.stream().filter(w -> projectId.equals(w.getProjectId())).collect(Collectors.toList()));
        CompletableFuture<Project> projectF = projectService.getProjectById(projectId);

        return CompletableFuture.allOf(tasksF, worklogsF, projectF).thenApply(v -> {
            List<Task> tasks = tasksF.join();
            List<WorkLog> worklogs = worklogsF.join();
            Project project = projectF.join();

            int totalTasks = tasks.size();
            int completedTasks = (int) tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus()) || "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
            double progress = totalTasks == 0 ? 0.0 : (completedTasks * 100.0 / totalTasks);

            Integer daysRemaining = null;
            if (project != null && project.getEndDate() != null) {
                LocalDate end = Instant.ofEpochMilli(project.getEndDate().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate today = LocalDate.now();
                long days = ChronoUnit.DAYS.between(today, end);
                daysRemaining = (int) Math.max(days, 0);
            }

            String riskLevel = "LOW";
            if (progress < 50.0 && (daysRemaining == null || daysRemaining < 7)) riskLevel = "HIGH";
            else if (progress < 75.0) riskLevel = "MEDIUM";

            ProgressReport pr = new ProgressReport();
            pr.setProjectId(projectId);
            pr.setProgressPercentage(round(progress));
            pr.setDaysRemaining(daysRemaining);
            pr.setRiskLevel(riskLevel);
            pr.setMilestones(Collections.emptyList());

            return pr;
        });
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
