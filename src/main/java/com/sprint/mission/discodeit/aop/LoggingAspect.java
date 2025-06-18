package com.sprint.mission.discodeit.aop;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

//    @Pointcut("execution(* com.sprint.mission.discodeit.controller..*(..))")
//    public void controllerMethods() {}

//    @Pointcut("execution(* com.sprint.mission.discodeit.service.basic.BasicUserService.create*(..)) || " +
//            "execution(* com.sprint.mission.discodeit.service.basic.BasicUserService.update*(..)) || " +
//            "execution(* com.sprint.mission.discodeit.service.basic.BasicUserService.delete*(..))")
//    public void basicUserServicePointcut() {}
//
//    @Pointcut("execution(* com.sprint.mission.discodeit.service.basic.BasicChannelService.create*(..)) || " +
//            "execution(* com.sprint.mission.discodeit.service.basic.BasicChannelService.update*(..)) || " +
//            "execution(* com.sprint.mission.discodeit.service.basic.BasicChannelService.delete*(..))")
//    public void basicChannelServicePointcut() {}
//
//    @Pointcut("execution(* com.sprint.mission.discodeit.service.basic.BasicMessageService.create*(..)) || " +
//            "execution(* com.sprint.mission.discodeit.service.basic.BasicMessageService.update*(..)) || " +
//            "execution(* com.sprint.mission.discodeit.service.basic.BasicMessageService.delete*(..))")
//    public void basicMessageServicePointcut() {}
//
//    @Pointcut("execution(* com.sprint.mission.discodeit.storage.LocalBinaryContentStorage.put*(..)) || " +
//            "execution(* com.sprint.mission.discodeit.storage.LocalBinaryContentStorage.download*(..))")
//    public void binaryContentStoragePointcut() {}

    @Pointcut("execution(* com.sprint.mission.discodeit.service.basic.BasicUserService.create*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicUserService.update*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicUserService.delete*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicChannelService.create*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicChannelService.update*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicChannelService.delete*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicMessageService.create*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicMessageService.update*(..)) || " +
            "execution(* com.sprint.mission.discodeit.service.basic.BasicMessageService.delete*(..)) || " +
            "execution(* com.sprint.mission.discodeit.storage.LocalBinaryContentStorage.put*(..)) || " +
            "execution(* com.sprint.mission.discodeit.storage.LocalBinaryContentStorage.download*(..))")
    public void businessServicePointcut() {}

//    @Pointcut("execution(* com.sprint.mission.discodeit.service..*(..))")
//    public void serviceMethods() {}

    @Around("businessServicePointcut()")
    public Object logBusinessEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[{}] 비즈니스 이벤트 시작 - 작업: {}, 매개변수: {}", className, methodName, Arrays.toString(args));

        try {
            Object result = joinPoint.proceed();
            log.info("[{}] 비즈니스 이벤트 성공 - 작업: {}, 결과: {}", className, methodName, result);
            return result;
        } catch (Throwable throwable) {
            log.error("[{}] 비즈니스 이벤트 실패 - 작업: {}, 예외: {}, 메시지: {}",
                    className, methodName, throwable.getClass().getSimpleName(), throwable.getMessage());
            throw throwable;
        }
    }

}
