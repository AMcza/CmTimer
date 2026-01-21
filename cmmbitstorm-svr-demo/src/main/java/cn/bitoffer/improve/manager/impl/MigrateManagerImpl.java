package cn.bitoffer.improve.manager.impl;

import cn.bitoffer.improve.common.conf.MigratorAppConf;
import cn.bitoffer.improve.enums.TaskStatus;
import cn.bitoffer.improve.enums.TimerStatus;
import cn.bitoffer.improve.exception.BusinessException;
import cn.bitoffer.improve.exception.ErrorCode;
import cn.bitoffer.improve.manager.MigratorManager;
import cn.bitoffer.improve.mapper.TaskMapper;
import cn.bitoffer.improve.mapper.TimerMapper;
import cn.bitoffer.improve.model.TaskModel;
import cn.bitoffer.improve.model.TimerModel;
import cn.bitoffer.improve.redis.ReetrantDistributeLock;
import cn.bitoffer.improve.redis.TaskCache;
import cn.bitoffer.improve.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Migrate通用流程：
 *  1.调用timer的激活接口时，会触发一次Migrate
 *  2.项目中系统定时任务，每隔一段时间，就会触发全局所有timer的Migrate
 */
@Service
@Slf4j
public class MigrateManagerImpl implements MigratorManager {

    @Autowired
    private TimerMapper timerMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ReetrantDistributeLock reetrantDistributeLock;

    @Autowired
    private MigratorAppConf migratorAppConf;

    @Autowired
    private TaskCache taskCache;

    @Override
    public void migrateTimer(TimerModel timerModel) {
        //校验状态
        if(timerModel.getStatus()!= TimerStatus.Enable.getStatus()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Timer非Enable状态，迁移失败,timerId:" + timerModel.getTimerId());
        }
        //获取批量执行时机
        CronExpression cronExpression;
        try{
            cronExpression=new CronExpression(timerModel.getCron());
        }catch (ParseException e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"解析Cron表达式失败:"+timerModel.getCron());
        }
        Date now=new Date();
        Date end= TimerUtils.GetForwardTwoMigrateStepEnd(now,migratorAppConf.getMigrateStepMinutes());
        //获取执行时机
        List<Long> executeTimes=TimerUtils.GetCronNextsBetween(cronExpression,now,end);
        if(CollectionUtils.isEmpty(executeTimes)){
            log.warn("获取执行时机 executeTimes 为空，timerId:{}",timerModel.getTimerId());
            return;
        }
        //执行时机加入数据库
        List<TaskModel> taskList=batchTasksFromTimer(timerModel,executeTimes);

        //基于timer_id+run_timer 唯一键,保证任务不被重复插入
        taskMapper.batchSave(taskList);

        //执行时机加入 redis Zset
        boolean cacheRes=taskCache.cacheSaveTasks(taskList);
        if(!cacheRes){
            log.error("Zset存储taskList失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ZSet存储taskList失败,timerId:"+timerModel.getTimerId());
        }
    }

    //批量给任务设置定时器
    private List<TaskModel> batchTasksFromTimer(TimerModel timerModel,List<Long> executeTimes){
        if(timerModel==null || CollectionUtils.isEmpty(executeTimes)){
            return null;
        }
        List<TaskModel> taskList=new ArrayList<>();
        for(Long runTimer: executeTimes){
            TaskModel task=new TaskModel();
            task.setApp(timerModel.getApp());
            task.setTimerId(timerModel.getTimerId());
            task.setRunTimer(runTimer);
            task.setStatus(TaskStatus.NotRun.getStatus());
            taskList.add(task);
        }
        return taskList;
    }
}
