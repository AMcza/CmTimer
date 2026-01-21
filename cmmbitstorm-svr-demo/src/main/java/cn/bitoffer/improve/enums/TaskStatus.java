package cn.bitoffer.improve.enums;

/**
 * 一个周期性的闹钟timer最终会生成一个单次触发的任务task
 */
public enum TaskStatus {
    NotRun(0),
    Running(1),
    Succeed(2),
    Failed(3);
    private TaskStatus(int status) {
        this.status = status;
    }
    private int status;

    public int getStatus() {
        return this.status;
    }
}
