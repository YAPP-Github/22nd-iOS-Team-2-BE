package com.pyonsnalcolor.aop;

import com.pyonsnalcolor.exception.PyonsnalcolorBatchException;
import lombok.extern.slf4j.Slf4j;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackAttachment;
import net.gpedro.integrations.slack.SlackField;
import net.gpedro.integrations.slack.SlackMessage;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Aspect
@Component
public class BatchExceptionAspect {

    private static final String SLACK_URL = "https://hooks.slack.com/services/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";

    @Pointcut("execution(* com.pyonsnalcolor.batch.service..*(..))")
    private void allBatchService() {}

    @AfterThrowing(value = "allBatchService()", throwing = "exception")
    public void catchBatchException(JoinPoint joinPoint, PyonsnalcolorBatchException exception) {
        log.error("[{}] 배치 실행 중 {}에서 예외가 발생하였습니다.", exception.getClass().getSimpleName(),
                joinPoint.getTarget().getClass().getSimpleName());
        log.error("[custom exception] {} {} ", exception.getErrorCode().name(), exception.getErrorCode().getMessage());
        log.error("[origin exception] {} {}", exception.getOriginException().getClass().getSimpleName(),
                exception.getOriginException().getMessage());

        sendErrorSlackAlarm(joinPoint, exception);
    }

    private void sendErrorSlackAlarm(JoinPoint joinPoint, PyonsnalcolorBatchException exception) {
        SlackApi api = new SlackApi(SLACK_URL);

        SlackMessage errorSlackMessage = createErrorSlackMessage(joinPoint, exception);
        api.call(errorSlackMessage);
    }

    private SlackMessage createErrorSlackMessage(JoinPoint joinPoint, PyonsnalcolorBatchException exception) {
        SlackMessage slackMessage = new SlackMessage();
        slackMessage.setText("Batch Error Occur");
        slackMessage.setIcon(":ghost:");
        slackMessage.setUsername("pyonsnalcolor");

        SlackAttachment slackAttachment = new SlackAttachment();
        slackAttachment.setFallback("Batch Error");
        slackAttachment.setTitle("배치 실행중 예외가 발생하였습니다.");
        slackAttachment.setColor("#ffff00");
        slackAttachment.setText(Arrays.toString(exception.getStackTrace()));
        slackAttachment.setFields (
                List.of(
                        new SlackField().setTitle("Occurred Time").setValue(LocalDateTime.now().toString()),
                        new SlackField().setTitle("Occurred Class")
                                .setValue(joinPoint.getTarget().getClass().getSimpleName()),
                        new SlackField().setTitle("Custom Message").setValue(exception.getErrorCode().getMessage()),
                        new SlackField().setTitle("Origin Exception")
                                .setValue(exception.getOriginException().getClass().getSimpleName()),
                        new SlackField().setTitle("Origin Exception Message")
                                .setValue(exception.getOriginException().getMessage())
                )
        );
        slackMessage.setAttachments(Collections.singletonList(slackAttachment));
        return slackMessage;
    }
}