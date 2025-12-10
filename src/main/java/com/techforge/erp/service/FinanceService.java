package com.techforge.erp.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.techforge.erp.model.Expense;
import com.techforge.erp.model.Invoice;
import com.techforge.erp.model.Payroll;
import com.techforge.erp.model.User;
import com.techforge.erp.model.WorkLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private final DatabaseReference payrollsRef;
    private final DatabaseReference invoicesRef;
    private final DatabaseReference expensesRef;

    private final WorkLogService workLogService;
    private final UserService userService;

    @Autowired
    public FinanceService(UserService userService, WorkLogService workLogService) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.payrollsRef = root.child("payrolls");
        this.invoicesRef = root.child("invoices");
        this.expensesRef = root.child("expenses");
        this.userService = userService;
        this.workLogService = workLogService;
    }

    public CompletableFuture<Payroll> calculatePayroll(String userId, int month, int year) {
        CompletableFuture<Payroll> future = new CompletableFuture<>();

        try {
            // fetch all worklogs then filter by userId and month/year
            workLogService.getAllWorkLogs().thenCompose(all -> {
                List<WorkLog> filtered = all.stream().filter(w -> {
                    if (w == null || w.getUserId() == null) return false;
                    if (!w.getUserId().equals(userId)) return false;
                    if (w.getWorkDate() == null) return false;
                    Calendar c = Calendar.getInstance();
                    c.setTime(w.getWorkDate());
                    int wm = c.get(Calendar.MONTH) + 1;
                    int wy = c.get(Calendar.YEAR);
                    return wm == month && wy == year;
                }).collect(Collectors.toList());

                // fetch user once
                return userService.getUserById(userId).thenApply(user -> new AbstractMap.SimpleEntry<>(user, filtered));
            }).thenAccept(pair -> {
                User user = pair.getKey();
                List<WorkLog> logs = pair.getValue();

                double totalRegular = 0.0;
                double totalOvertime = 0.0;

                for (WorkLog wl : logs) {
                    double regularHours = wl.getRegularHours() == null ? 0.0 : wl.getRegularHours();
                    double overtimeHours = wl.getOvertimeHours() == null ? 0.0 : wl.getOvertimeHours();

                    double baseSnapshot = wl.getBaseSalarySnapshot() == null ? 0.0 : wl.getBaseSalarySnapshot();

                    // determine hourly rate based on user's salaryType if available, else assume monthly
                    String salaryType = (user != null) ? user.getSalaryType() : null;
                    double hourlyRate;
                    if ("hourly".equalsIgnoreCase(salaryType)) {
                        hourlyRate = baseSnapshot; // snapshot already hourly rate
                    } else {
                        // default to monthly
                        hourlyRate = baseSnapshot / 160.0;
                    }

                    double regPay = hourlyRate * regularHours;

                    double otRate = wl.getHourlyRateOTSnapshot() == null ? hourlyRate : wl.getHourlyRateOTSnapshot();
                    double otPay = otRate * overtimeHours;

                    totalRegular += regPay;
                    totalOvertime += otPay;
                }

                double totalPay = totalRegular + totalOvertime;

                Payroll payroll = new Payroll();
                String key = payrollsRef.push().getKey();
                if (key == null) {
                    future.completeExceptionally(new IllegalStateException("Unable to generate payroll key"));
                    return;
                }
                payroll.setId(key);
                payroll.setUserId(userId);
                payroll.setMonth(month);
                payroll.setYear(year);
                payroll.setBaseSalary(user != null ? user.getBaseSalary() : null);
                payroll.setOvertimePay(totalOvertime);
                payroll.setTotalPay(totalPay);
                payroll.setPaid(false);
                payroll.setTransactionId(null);

                payrollsRef.child(key).setValueAsync(payroll).addListener(() -> future.complete(payroll), Runnable::run);

            }).exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public CompletableFuture<Invoice> createInvoice(Invoice invoice) {
        CompletableFuture<Invoice> future = new CompletableFuture<>();
        try {
            String key = (invoice.getId() != null && !invoice.getId().isEmpty()) ? invoice.getId() : invoicesRef.push().getKey();
            if (key == null) {
                future.completeExceptionally(new IllegalStateException("Unable to generate key for invoice"));
                return future;
            }
            invoice.setId(key);
            invoicesRef.child(key).setValueAsync(invoice).addListener(() -> future.complete(invoice), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Expense> createExpense(Expense expense) {
        CompletableFuture<Expense> future = new CompletableFuture<>();
        try {
            String key = (expense.getId() != null && !expense.getId().isEmpty()) ? expense.getId() : expensesRef.push().getKey();
            if (key == null) {
                future.completeExceptionally(new IllegalStateException("Unable to generate key for expense"));
                return future;
            }
            expense.setId(key);
            expensesRef.child(key).setValueAsync(expense).addListener(() -> future.complete(expense), Runnable::run);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}
