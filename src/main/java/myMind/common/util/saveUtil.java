package myMind.common.util;

import myMind.componet.MindMap;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class saveUtil {

    private static final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

    public static final Set<String> set = new HashSet<>();

    public static void saveLater(MindMap mindMap) {
        if (scheduler.isShutdown()) {
            return;
        }
        String filePath = mindMap.getFilePath();
        if (!set.contains(filePath)) {
            set.add(filePath);
            scheduler.schedule(() -> {
                FileUtil.saveFile(new File(filePath), mindMap, false);
                set.remove(filePath);
            }, 1, TimeUnit.SECONDS);
        }
    }

    // ScheduledExecutorService 创建的是非守护线程，会阻止 JVM 自然退出，需要关闭
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

}
