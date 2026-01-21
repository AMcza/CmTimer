package cn.bitoffer.improve.common.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
//迁移器配置信息
@Component
public class MigratorAppConf {
    //迁移器线程数
    @Value("${migrator.workersNum}")
    private int workersNum;
    //迁移器间隔时间单位长
    @Value("${migrator.migrateStepMinutes}")
    private int migrateStepMinutes;
    //迁移成功过期时间
    @Value("${migrator.migrateSuccessExpireMinutes}")
    private int migrateSuccessExpireMinutes;
    //迁移锁过期时间
    @Value("${migrator.migrateTryLockMinutes}")
    private int migrateTryLockMinutes;
    //定时器详情缓存时间
    @Value("${migrator.timerDetailCacheMinutes}")
    private int timerDetailCacheMinutes;

    public int getWorkersNum(){
        return workersNum;
    }

    public void setWorkersNum(int workersNum){
        this.workersNum=workersNum;
    }

    public int getMigrateStepMinutes(){
        return migrateStepMinutes;
    }

    public void setMigrateStepMinutes(int migrateStepMinutes){
        this.migrateStepMinutes=migrateStepMinutes;
    }

    public void getMigrateSuccessExpireMinutes(int migrateSuccessExpireMinutes){
        this.migrateSuccessExpireMinutes=migrateSuccessExpireMinutes;
    }

    public int getMigrateTryLockMinutes(){
        return migrateTryLockMinutes;
    }

    public void setMigrateTryLockMinutes(int migrateTryLockMinutes){
        this.migrateTryLockMinutes=migrateTryLockMinutes;
    }
}
