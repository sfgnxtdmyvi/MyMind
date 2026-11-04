package myMind.common.util;

import myMind.componet.MindMap;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SaveUtil {

    private static final ScheduledThreadPoolExecutor scheduler =
            new ScheduledThreadPoolExecutor(1);

    // 待执行的延迟保存任务
    private static final Map<MindMap, ScheduledFuture> pendingTasks = new ConcurrentHashMap<>();

    public static void saveLater(MindMap mindMap) {
        // 保证同一 mindMap 只调度一个任务（原子性）
        pendingTasks.compute(mindMap, (mm, existing) -> {
            // 已有待执行任务
            if (existing != null) {
                return existing;
            }

            return scheduler.schedule(() -> {
                FileUtil.saveFile(new File(mindMap.getFilePath()), mindMap, false);
                pendingTasks.remove(mindMap);
            }, 10, TimeUnit.SECONDS);
        });
    }

    public static void saveNow(MindMap mindMap) {
        ScheduledFuture future = pendingTasks.remove(mindMap);
        // 没有任务
        if (future == null) {
            return;
        }
        // 任务在执行中
        if (!future.cancel(false)) {
            return;
        }
        FileUtil.saveFile(new File(mindMap.getFilePath()), mindMap, false);
    }

    // ScheduledExecutorService 创建的是非守护线程，会阻止 JVM 自然退出，需要关闭
    public static void shutdown() {
        scheduler.shutdown();
    }

}