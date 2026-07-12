package myMind.common.util;

import myMind.componet.MindMap;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduleUtil {
    private static final ScheduledThreadPoolExecutor scheduler;

    static {
        scheduler = new ScheduledThreadPoolExecutor(3);
        scheduler.setRemoveOnCancelPolicy(true);
    }

    private static final Map<String, ScheduledFuture<?>> fileSaveFutures = new ConcurrentHashMap<>();

    public static void scheduleAutoSave(String filePath, MindMap mindMap) {
        if (fileSaveFutures.containsKey(filePath)) {
            return;
        }

        WeakReference<MindMap> weakReference = new WeakReference<>(mindMap);
        // scheduleAtFixedRate + 匿名类创建 Runnable 任务 + 传递强引用 = 内存泄漏
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                    FileUtil.saveFileSilence(new File(filePath), weakReference.get());
                },
                60, 60, TimeUnit.SECONDS);
        fileSaveFutures.put(filePath, future);
    }

    public static void cancelSchedule(String filePath) {
        ScheduledFuture<?> future = fileSaveFutures.remove(filePath);
        future.cancel(true);
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
