package cn.bitoffer.improve.controller;

import cn.bitoffer.api.dto.xtimer.TimerDTO;
import cn.bitoffer.common.model.ResponseEntity;
import cn.bitoffer.improve.service.XTimerService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/xtimer")
public class XtimerWebController {

    @Resource
    private XTimerService xTimerService;

    @PostMapping("/createTimer")
    public ResponseEntity<Long> createTimer(@RequestBody TimerDTO timerDTO){
        Long timerId=xTimerService.CreateTimer(timerDTO);
        return ResponseEntity.ok(timerId);
    }

    @GetMapping(value="/enableTimer")
    public ResponseEntity<String> enableTimer(
            @RequestParam(value="app") String app,
            @RequestParam(value="timerId") Long timerId,
            @RequestHeader MultiValueMap<String,String> headers
            ){
        xTimerService.EnableTimer(app,timerId);
        return ResponseEntity.ok("ok");
    }
}
