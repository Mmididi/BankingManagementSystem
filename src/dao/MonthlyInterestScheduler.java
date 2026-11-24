package dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonthlyInterestScheduler implements AutoCloseable {

    public static final ZoneId BANK_ZONE = ZoneId.of("Africa/Gaborone");

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "monthly-interest");
                thread.setDaemon(true);
                return thread;
            });

    public void start() {
        executor.scheduleWithFixedDelay(this::checkMonthEnd, 0, 15, TimeUnit.MINUTES);
    }

    private void checkMonthEnd() {
        ZonedDateTime now = ZonedDateTime.now(BANK_ZONE);

        if (now.getDayOfMonth() != now.toLocalDate().lengthOfMonth()
                || now.getHour() < 23) {
            return;
        }

        try {
            new TransactionDAO().applyMonthlyInterest();
        } catch (Exception exception) {
            System.err.println("Monthly interest failed: " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}