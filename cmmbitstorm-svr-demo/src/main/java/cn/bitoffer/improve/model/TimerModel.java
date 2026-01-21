package cn.bitoffer.improve.model;

import cn.bitoffer.api.dto.xtimer.NotifyHTTPParam;
import cn.bitoffer.api.dto.xtimer.TimerDTO;
import cn.bitoffer.common.model.BaseModel;
import cn.bitoffer.improve.utils.JSONUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * 存储定时设置(闹钟timer)
 */
@Data
public class TimerModel extends BaseModel implements Serializable {

    private Long timerId;

    private String app;

    private String name;

    private int status;

    private String cron;

    private String notifyHTTPParam;

    public static TimerModel voToObj(TimerDTO timerDTO){
        if(timerDTO==null){
            return null;
        }
        TimerModel timerModel = new TimerModel();
        timerModel.setApp(timerDTO.getApp());
        timerModel.setCron(timerDTO.getCron());
        timerModel.setName(timerDTO.getName());
        timerModel.setStatus(timerDTO.getStatus());
        timerModel.setNotifyHTTPParam(JSONUtil.toJsonString(timerDTO.getNotifyHTTPParam()));
        return timerModel;
    }

    public static TimerDTO objToVo(TimerModel timerModel){
        if(timerModel==null){
            return null;
        }
        TimerDTO timerDTO = new TimerDTO();
        timerDTO.setApp(timerModel.getApp());
        timerDTO.setTimerId(timerModel.getTimerId());
        timerDTO.setName(timerModel.getName());
        timerDTO.setStatus(timerModel.getStatus());
        timerDTO.setCron(timerModel.getCron());
        NotifyHTTPParam httpParam=JSONUtil.parseObject(timerModel.getNotifyHTTPParam(),NotifyHTTPParam.class);
        timerDTO.setNotifyHTTPParam(httpParam);

        BeanUtils.copyProperties(timerModel,timerDTO);
        return timerDTO;
    }


}
