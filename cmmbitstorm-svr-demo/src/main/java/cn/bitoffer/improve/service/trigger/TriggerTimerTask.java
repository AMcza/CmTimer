package cn.bitoffer.improve.service.trigger;

import cn.bitoffer.improve.common.conf.TriggerAppConf;
import cn.bitoffer.improve.enums.TaskStatus;
import cn.bitoffer.improve.mapper.TaskMapper;
import cn.bitoffer.improve.model.TaskModel;
import cn.bitoffer.improve.redis.TaskCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class TriggerTimerTask extends TimerTask {

    private TriggerAppConf triggerAppConf;

    private TriggerPoolTask triggerPoolTask;

    private TaskCache taskCache;

    private TaskMapper taskMapper;

    private CountDownLatch latch;

    private Long count=0L;

    private Date startTime;

    private Date endTime;

    private String minuteBucketKey;

    public TriggerTimerTask(TriggerAppConf triggerAppConf,
                            TriggerPoolTask triggerPoolTask,
                            TaskCache taskCache,TaskMapper taskMapper,
                            CountDownLatch latch,
                            Date startTime,
                            Date endTime,
                            String minuteBucketKey){
        this.triggerAppConf=triggerAppConf;
        this.triggerPoolTask=triggerPoolTask;
        this.taskCache=taskCache;
        this.taskMapper=taskMapper;
        this.latch=latch;
        this.startTime=startTime;
        this.endTime=endTime;
        this.minuteBucketKey=minuteBucketKey;
    }

    @Override
    public void run() {
        Date tStart=new Date(startTime.getTime()+count*triggerAppConf.getZrangeGapSeconds()*1000L);
        //推出条件: tStart>=endTime表示执行完成
        if(tStart.compareTo(endTime)>=0){
            latch.countDown();
            return;
        }
        //处理1s任务: [tstart+1秒] 这个范围的任务。
        try{
            handleBatch(tStart,new Date(tStart.getTime()+triggerAppConf.getZrangeGapSeconds()*1000L));
        }catch (Exception e){
            log.error("handleBatch Error。mintueBucketKey"+minuteBucketKey+",tStatTime:"+startTime+",e"+e);
        }
        count++;
    }

    private void handleBatch(Date start,Date end){
        //获取待触发的任务
        List<TaskModel> tasks=getTasksByTime(start,end);
        if(CollectionUtils.isEmpty(tasks)){
            return;
        }
        //从ZSet捞到一批触发任务，现在遍历并执行
        for(TaskModel task:tasks){
            try{
                if(task==null){
                    continue;
                }
                //调用执行模块Executor
                triggerPoolTask.runExecutor(task);
            }catch (Exception e){
                log.error("executor run task error,task:"+task.toString());
            }
        }
    }

    //根据时间获取任务
    private List<TaskModel> getTasksByTime(Date start,Date end){

        List<TaskModel> tasks=new ArrayList<>();
        //先走缓存
        try{
            tasks=taskCache.getTasksFromCache(minuteBucketKey,start.getTime(),end.getTime());
        }catch (Exception e){
            //缓存miss，查询数据库
            log.error("getTasksByConditions error:{}",e);

            try{
                tasks=taskMapper.getTasksByTimeRange(start.getTime(),end.getTime()-1, TaskStatus.NotRun.getStatus());
            }catch (Exception e1){
                log.error("getTasksByConditions error:{}",e1);
            }
        }
        return tasks;
    }
}
