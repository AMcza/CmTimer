package cn.bitoffer.improve.mapper;

import cn.bitoffer.improve.model.TaskModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface TaskMapper {
    /**
     * 批量插入任务taskModel
     * @param taskList
     */
    void batchSave(@Param("taskList") List<TaskModel> taskList);

    /**
     * 根据timerid删除taskModel
     * @param taskId
     */
    void deleteById(@Param("taskId") Long taskId);

    /**
     * 更新TimerModel
     * @param taskModel
     */
    void update(@Param("taskModel") TaskModel taskModel);

    /**
     * 根据时间区间查询task
      * @param startTime
     * @param endTime
     * @param taskStatus
     * @return
     */
    List<TaskModel> getTasksByTimeRange(@Param("startTime") Long startTime, @Param("endTime") Long endTime,@Param("taskStatus")int taskStatus);

    /**
     * 根据timerid查询task
     * @param timerId
     * @param runTimer
     * @return
     */
    TaskModel getTasksByTimerIdUnix(@Param("timerId")Long timerId,@Param("runTimer")Long runTimer);
}
