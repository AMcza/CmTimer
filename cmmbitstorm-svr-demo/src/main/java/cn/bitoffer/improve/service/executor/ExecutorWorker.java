package cn.bitoffer.improve.service.executor;

import cn.bitoffer.api.dto.xtimer.NotifyHTTPParam;
import cn.bitoffer.api.dto.xtimer.TimerDTO;
import cn.bitoffer.improve.enums.TaskStatus;
import cn.bitoffer.improve.enums.TimerStatus;
import cn.bitoffer.improve.exception.BusinessException;
import cn.bitoffer.improve.exception.ErrorCode;
import cn.bitoffer.improve.mapper.TaskMapper;
import cn.bitoffer.improve.mapper.TimerMapper;
import cn.bitoffer.improve.model.TaskModel;
import cn.bitoffer.improve.model.TimerModel;
import cn.bitoffer.improve.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

/**
 * 触发器
 */
@Component
@Slf4j
public class ExecutorWorker {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TimerMapper timerMapper;

    public void work(String timerIDUnixKey){
        List<Long> longSet= TimerUtils.SplitTimerIDUnix(timerIDUnixKey);
        if(longSet.size()!=2){
            log.error("splitTimerIDUnix 错误,timerIDUnix:{}",timerIDUnixKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"splitTimerIDUnix 错误,timerIDUnix:"+timerIDUnixKey);
        }
        Long timerId=longSet.get(0);
        Long timerUnix=longSet.get(1);

        //查询出任务,判断是否执行过了。避免重复执行
        TaskModel task=taskMapper.getTasksByTimerIdUnix(timerId,timerUnix);
        if(task.getStatus()!= TaskStatus.NotRun.getStatus()){
            log.warn("重复执行任务: timerId:"+timerId+",runtimer:"+timerUnix);
            return;
        }
        //执行回调
        executeAndPostProcess(task,timerId,timerUnix);

    }

    private void executeAndPostProcess(TaskModel taskModel,Long timerId,Long unix){
        //查询timerModel
        TimerModel timerModel=timerMapper.getTimerById(timerId);
        if(timerModel==null){
            log.error("执行回调错误,找不到对应的timer。timerid"+timerId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"执行回调错误,找不到对应的timer。timerId:"+timerId);
        }

        //判断定时器是否还处于激活状态,如果为 去激活状态，就不回调了。
        if(timerModel.getStatus()!= TimerStatus.Enable.getStatus()){
            log.warn("Timer已经处于去激活状态。timerId:"+timerId);
            return;
        }

        //触发时间的误差误差时间
        int gapTime=(int)(new Date().getTime()-taskModel.getRunTimer());
        taskModel.setCostTime(gapTime);

        //执行http回调,通知业务放
        ResponseEntity<String> resp=null;
        try{
            resp=executeTimerCallBack(timerModel);
        }catch (Exception e){
            log.error("执行回调失败,抛出异常e:"+e);
        }
        //后置处理，更新Timer执行结果
        if(resp!=null){
            taskModel.setStatus(TaskStatus.Failed.getStatus());
            taskModel.setOutput("resp is null");
        }else if(resp.getStatusCode().is2xxSuccessful()){
            taskModel.setStatus(TaskStatus.Succeed.getStatus());
            taskModel.setOutput(resp.toString());
        }else{
            taskModel.setStatus(TaskStatus.Failed.getStatus());
            taskModel.setOutput(resp.toString());
        }
        taskMapper.update(taskModel);
    }

    private ResponseEntity<String> executeTimerCallBack(TimerModel timerModel){

        TimerDTO timerDTO=TimerModel.objToVo(timerModel);
        NotifyHTTPParam httpParam=timerDTO.getNotifyHTTPParam();
        RestTemplate restTemplate=new RestTemplate();
        ResponseEntity<String> resp=null;
        switch (httpParam.getMethod()){
            case "POST":
                resp=restTemplate.postForEntity(httpParam.getUrl(),httpParam.getBody(),String.class);
            default:
                log.error("不支持的httpMethod");
                break;
        }
        StringBuffer sb=new StringBuffer();
        HttpStatus statusCode=resp.getStatusCode();
        if(!statusCode.is2xxSuccessful()){
            log.error("http 回调失败:"+resp);
        }
        return resp;
    }
}
