package com.duyell.communityfreshdelivery.common.exception;

import com.duyell.communityfreshdelivery.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>全局异常拦截器</h2>
 *
 * <p>通过 {@link RestControllerAdvice @RestControllerAdvice} 拦截所有 Controller 抛出的异常，
 * 统一包装为 {@link Result} 格式返回，Controller 层不再需要手写 try-catch.</p>
 *
 * <h3>拦截优先级</h3>
 * <ol>
 *   <li>{@link BusinessException} — Service 主动抛出的业务异常，message 直接返回给用户</li>
 *   <li>{@link MethodArgumentNotValidException} — {@code @Valid} 触发的参数校验失败，拼接字段级错误</li>
 *   <li>{@link Exception} — 兜底，记录完整堆栈，前端只收到"服务器内部错误"</li>
 * </ol>
 *
 * <h3>扩展指南</h3>
 * <p>需要更细粒度拦截时（如 HttpClientErrorException、AccessDeniedException），
 * 直接在本类新增 {@code @ExceptionHandler} 方法即可.</p>
 *
 * @author duyell
 * @since 2026-05-31
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 — message 直接返回给用户，日志记 warn 级别（可控异常，无需 ERROR）.
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 — 将字段级错误拼成可读字符串返回.
     * <p>返回示例：{@code "phone: 手机号不能为空; password: 密码长度至少6位"}</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验异常: {}", msg);
        return Result.fail(400, msg);
    }

    /**
     * 兜底处理 — 未预料的异常.
     * <p>日志记录完整堆栈（便于排查），前端只看到通用提示（不暴漏内部信息）.</p>
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("服务器内部错误");
    }
}
