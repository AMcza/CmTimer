package cn.bitoffer.improve.service.migrator;

import cn.bitoffer.common.redis.ReentrantDistributeLock;
import cn.bitoffer.improve.common.conf.MigratorAppConf;
import cn.bitoffer.improve.enums.TimerStatus;
import cn.bitoffer.improve.manager.MigratorManager;
import cn.bitoffer.improve.mapper.TimerMapper;
import cn.bitoffer.improve.model.TimerModel;
import cn.bitoffer.improve.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 作用：
 *  定时
 */
@Component
@Slf4j
public class MigratorWorker {

    @Autowired
    private TimerMapper timerMapper;

    @Autowired
    private MigratorAppConf migratorAppConf;

    @Autowired
    private MigratorManager migratorManager;

    @Autowired
    private ReentrantDistributeLock reentrantDistributeLock;

    @Scheduled(fixedRate = 10*1000) //60*60*10000 一小时执行一次
    public void work(){
        log.info("开始迁移时间:"+ LocalDateTime.now());
        Date startHour=getStartHour(new Date());
        String lockToken= TimerUtils.GetTokenStr();
        boolean ok=reentrantDistributeLock.lock(
                TimerUtils.GetMigratorLockKey(startHour),
                lockToken,
                60L*migratorAppConf.getMigrateTryLockMinutes());
        if(!ok){
            log.warn("migrator get lock failed!"+TimerUtils.GetMigratorLockKey(startHour));
            return;
        }
        //迁移
        migrate();
    }

    private Date getStartHour(Date date){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH");
        try{
            return sdf.parse(sdf.format(date));
        }catch (ParseException e){
            throw new RuntimeException(e);
        }
    }

    private void migrate(){
        //获取的激活的定时器
        List<TimerModel> timers=timerMapper.getTimersByStatus(TimerStatus.Enable.getStatus());
        if(CollectionUtils.isEmpty(timers)){
            log.info("migrate timers is empty");
            return;
        }
        for(TimerModel timerModel: timers){
            //根据定时器：生成执行时间点 保存到mysql和redis中
            migratorManager.migrateTimer(timerModel);
        }
    }
}
