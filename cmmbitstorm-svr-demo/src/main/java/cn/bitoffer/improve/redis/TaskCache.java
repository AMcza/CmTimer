package cn.bitoffer.improve.redis;

import cn.bitoffer.improve.common.conf.SchedulerAppConf;
import cn.bitoffer.improve.exception.BusinessException;
import cn.bitoffer.improve.exception.ErrorCode;
import cn.bitoffer.improve.model.TaskModel;
import cn.bitoffer.improve.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class TaskCache {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private SchedulerAppConf schedulerAppConf;

    public String GetTableName(TaskModel taskModel){
        int maxBucket=schedulerAppConf.getBucketsNum();

        StringBuilder sb=new StringBuilder();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeStr=sdf.format(taskModel.getRunTimer());
        long index=taskModel.getTimerId()%maxBucket;
        return sb.append(timeStr).append("_").append(index).toString();
    }

    //缓存保存任务
    public boolean cacheSaveTasks(List<TaskModel> taskList){
        try{
            SessionCallback sessionCallback=new SessionCallback() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    redisTemplate.multi();
                    for(TaskModel task: taskList){
                        long unix=task.getRunTimer();
                        String tableName=GetTableName(task);
                        redisTemplate.opsForZSet().add(
                                tableName,
                                TimerUtils.UnionTimerIDUnix(task.getTimerId(),unix),
                                unix);
                    }
                    return operations.exec();
                }
            };
            redisTemplate.execute(sessionCallback);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //从缓冲区获取任务
    public List<TaskModel> getTasksFromCache(String key,long start,long end){
        List<TaskModel> tasks=new ArrayList<>();
        //ZSet获取1s范围的任务
        Set<Object> timerIDUnixs=redisTemplate.opsForZSet().rangeByScore(key,start,end-1);
        if(CollectionUtils.isEmpty(timerIDUnixs)){
            return tasks;
        }
        for(Object timerIDUnixObj: timerIDUnixs){
            TaskModel task=new TaskModel();
            String timerIDUnix=(String)timerIDUnixObj;
            List<Long> longSet=TimerUtils.SplitTimerIDUnix(timerIDUnix);
            if(longSet.size()!=2){
                log.error("splitTimerIDUnix 错误,timerIDUnix:"+timerIDUnix);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"splitTimerIDUnix 错误");
            }
            task.setTimerId(longSet.get(0));
            task.setRunTimer(longSet.get(1));
            tasks.add(task);
        }
        return tasks;
    }
}
