package cn.bitoffer.improve.service.trigger;

import cn.bitoffer.common.redis.ReentrantDistributeLock;
import cn.bitoffer.improve.model.TaskModel;
import cn.bitoffer.improve.service.executor.ExecutorWorker;
import cn.bitoffer.improve.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
//触发器线程池--调用执行器，执行任务回调
@Slf4j
@Component
public class TriggerPoolTask {

    @Autowired
    private ReentrantDistributeLock reentrantDistributeLock;

    @Autowired
    private ExecutorWorker executorWorker;

    @Async("triggerPool")
    public void runExecutor(TaskModel task){
        if(task==null){
            return;
        }
        log.info("start runExecutor");

        executorWorker.work(TimerUtils.UnionTimerIDUnix(task.getTimerId(),task.getRunTimer()));

        log.info("end executeAsync");
    }
}
