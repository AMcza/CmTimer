package cn.bitoffer.improve.service.impl;

import cn.bitoffer.api.dto.xtimer.TimerDTO;
import cn.bitoffer.common.redis.ReentrantDistributeLock;
import cn.bitoffer.improve.enums.TimerStatus;
import cn.bitoffer.improve.exception.BusinessException;
import cn.bitoffer.improve.exception.ErrorCode;
import cn.bitoffer.improve.manager.MigratorManager;
import cn.bitoffer.improve.mapper.TimerMapper;
import cn.bitoffer.improve.model.TimerModel;
import cn.bitoffer.improve.service.XTimerService;
import cn.bitoffer.improve.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class XTimerServiceImpl implements XTimerService {

    @Autowired
    private TimerMapper timerMapper;

    @Autowired
    private ReentrantDistributeLock reentrantDistributeLock;

    @Autowired
    private MigratorManager migratorManager;

    private static final int defaultGapSeconds=3;

    /**
     * 创建定时器
     * @param timerDTO
     * @return
     */
    @Override
    public Long CreateTimer(TimerDTO timerDTO) {
        boolean isValidCron= CronExpression.isValidExpression(timerDTO.getCron());
        if(!isValidCron){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"invalid cron");
        }

        TimerModel timerModel=TimerModel.voToObj(timerDTO);
        if(timerDTO==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        timerMapper.save(timerModel);
        return timerModel.getTimerId();
    }

    /**
     * 获取定时器
     * @param app
     * @param id
     * @return
     */
    @Override
    public TimerDTO GetTimer(String app, long id) {
        TimerModel timerModel=timerMapper.getTimerById(id);
        TimerDTO timerDTO=TimerModel.objToVo(timerModel);
        return timerDTO;
    }

    /**
     * 更新定时器
     * @param timerDTO
     */
    @Override
    public void Update(TimerDTO timerDTO) {
        TimerModel timerModel=TimerModel.voToObj(timerDTO);
        if(timerDTO==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        timerMapper.update(timerModel);
    }

    @Override
    public void EnableTimer(String app, long id) {
        String lockToken=TimerUtils.GetTokenStr();
        boolean ok=reentrantDistributeLock.lock(
                TimerUtils.GetCreateLockKey(app),
                lockToken,
                defaultGapSeconds);
        if(!ok){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"激活/去激活操作过于频繁,请稍后再试！");
        }
        //激活逻辑
        doEnableTimer(id);
    }

    @Transactional
    public void doEnableTimer(long id){
        //1.数据库获取定时器Timer
        TimerModel timerModel=timerMapper.getTimerById(id);
        if(timerModel==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"激活失败,timer不存在");
        }
        //2.校验状态
        if(timerModel.getStatus()== TimerStatus.Enable.getStatus()){
            log.warn("Timer非Unable状态,激活失败，timerId:"+timerModel.getTimerId());
        }
        //3.修改状态
        timerModel.setStatus(TimerStatus.Enable.getStatus());
        timerMapper.update(timerModel);
        //迁移数据
        migratorManager.migrateTimer(timerModel);
    }

    @Override
    public void UnEnableTimer(String app, long id) {
        String lockToken= TimerUtils.GetTokenStr();
        boolean ok=reentrantDistributeLock.lock(
                TimerUtils.GetCreateLockKey(app),
                lockToken,
                defaultGapSeconds);
        if(!ok){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"激活/去激活操作过于频繁,请稍后再试！");
        }
        //去激活逻辑
        doUnEnableTimer(id);
    }

    @Transactional
    public void doUnEnableTimer(Long id){
        //1.数据库获取Timer
        TimerModel timerModel=timerMapper.getTimerById(id);
        //2.校验状态
        if(timerModel.getStatus()!=TimerStatus.Unable.getStatus()){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"Timer非Unable状态,去激活失败,id："+id);
        }
        timerModel.setStatus(TimerStatus.Unable.getStatus());
        timerMapper.update(timerModel);
    }

    @Override
    public List<TimerDTO> GetAppTimers(String app) {
        return null;
    }

    @Override
    public void DeleteTimer(String app, long id) {
        String lockToken=TimerUtils.GetTokenStr();
        boolean ok=reentrantDistributeLock.lock(
                TimerUtils.GetCreateLockKey(app),
                lockToken,
                defaultGapSeconds
        );
        if(!ok){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建/删除操作过于频繁,请稍后再试！");
        }

        timerMapper.deleteById(id);
    }

}
