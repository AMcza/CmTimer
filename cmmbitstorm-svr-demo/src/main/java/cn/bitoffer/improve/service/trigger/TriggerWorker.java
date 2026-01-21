package cn.bitoffer.improve.service.trigger;

import cn.bitoffer.improve.common.conf.TriggerAppConf;
import cn.bitoffer.improve.mapper.TaskMapper;
import cn.bitoffer.improve.redis.TaskCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;

@Component
@Slf4j
public class TriggerWorker {

    @Autowired
    private TriggerAppConf triggerAppConf;

    @Autowired
    private TriggerPoolTask triggerPoolTask;

    @Autowired
    private TaskCache taskCache;

    @Autowired
    private TaskMapper taskMapper;

    public void work(String minuteBucketKey){
        //进行为时一分钟的zrange
        Date startTime=getStartMinute(minuteBucketKey);
        Date endTime=new Date(startTime.getTime()+60000);

        CountDownLatch latch=new CountDownLatch(1);
        Timer timer=new Timer("Timer");
        TriggerTimerTask task=new TriggerTimerTask(
                triggerAppConf,triggerPoolTask,taskCache,taskMapper,latch,startTime,endTime,minuteBucketKey);
        //以固定速率周期性的执行任务
        timer.scheduleAtFixedRate(task,0L,triggerAppConf.getZrangeGapSeconds()*1000L);
        try{
            latch.await();
        }catch (InterruptedException e){
            log.error("执行TriggerTimerTask异常中断,task:"+task);
        }finally {
            timer.cancel();
        }
    }

    private Date getStartMinute(String minuteBucketKey){
        String[] timeBucket=minuteBucketKey.split("_");
        if(timeBucket.length!=2){
            log.error("TriggerWorker getStatMinute 错误");
            return null;
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date startMinute=null;
        try{
            startMinute=sdf.parse(timeBucket[0]);
        }catch (ParseException e){
            log.error("TriggerWorker getStatMinute 错误");
        }
        return startMinute;
    }
}
