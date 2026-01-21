package cn.bitoffer.improve.enums;

/**
 * 主要用于标识用户创建的定时任务是激活状态还是去激活状态
 */
public enum TimerStatus {
    Unable(1),
    Enable(2),;
    private TimerStatus(int status) {
        this.status = status;
    }
    private int status;

    public int getStatus() {
        return this.status;
    }

    public static TimerStatus getTimerStatus(int status){
        for (TimerStatus value:TimerStatus.values()) {
            if(value.status == status){
                return value;
            }
        }
        return null;
    }
}
