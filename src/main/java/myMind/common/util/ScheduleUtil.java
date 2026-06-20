package myMind.common.util;

import myMind.componet.MindMap;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduleUtil {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private static final Map<String, ScheduledFuture<?>> fileSaveFutures = new ConcurrentHashMap<>();

    public static void scheduleAutoSave(String filePath, MindMap mindMap) {
        if (fileSaveFutures.containsKey(filePath)) {
            return;
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() ->
                FileUtil.saveFileSilence(new File(filePath), mindMap), 60, 60, TimeUnit.SECONDS);
        fileSaveFutures.put(filePath, future);
    }

    public static void cancelSchedule(String filePath) {
        ScheduledFuture<?> future = fileSaveFutures.remove(filePath);
        future.cancel(false);
    }

    // ScheduledExecutorService 创建的是非守护线程，会阻止 JVM 自然退出，需要关闭
    public static void cancelSchedule() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public static boolean containsScheduled(String filePath) {
        return fileSaveFutures.containsKey(filePath);
    }
}
