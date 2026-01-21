package cn.bitoffer.improve.model;

import cn.bitoffer.common.model.BaseModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 表示单次触发的任务
 */
@Data
public class TaskModel extends BaseModel implements Serializable {

    private Integer taskId;

    private String app;

    private Long timerId;

    private String output;

    private Long runTimer;

    private int costTime;

    private int status;

}
