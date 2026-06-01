package com.duyell.communityfreshdelivery.common.result;

import lombok.Data;

/**
 * <h2>统一响应体</h2>
 *
 * <p>所有 Controller 的返回值 <b>必须</b> 包装为此类型，前端统一解析 {@code {code, message, data}} 结构，
 * 无需再依赖 HTTP 状态码判断成败.</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 成功 — 无数据
 * @GetMapping("/ping")
 * public Result<Void> ping() {
 *     return Result.ok();
 * }
 *
 * // 成功 — 带数据
 * @GetMapping("/user/{id}")
 * public Result<UserVO> getUser(@PathVariable Long id) {
 *     return Result.ok(userService.getById(id));
 * }
 *
 * // 成功 — 自定义消息
 * @PostMapping("/order")
 * public Result<OrderVO> create(@RequestBody OrderDTO dto) {
 *     return Result.ok("下单成功", orderService.create(dto));
 * }
 *
 * // 失败 — 通用错误
 * @GetMapping("/data")
 * public Result<?> data() {
 *     return Result.fail("数据加载失败");
 * }
 *
 * // 失败 — 指定错误码
 * return Result.fail(400, "手机号格式不正确");
 * }</pre>
 *
 * <h3>约定</h3>
 * <ul>
 *   <li>{@code code=200} 表示成功，其余为错误码</li>
 *   <li>业务异常由 {@code GlobalExceptionHandler} 统一拦截，Service 层直接 throw 即可</li>
 *   <li>{@code message} 始终面向用户，不要暴漏堆栈信息</li>
 * </ul>
 *
 * @param <T> 响应数据的类型
 * @author duyell
 * @since 2026-05-31
 */
@Data
public class Result<T> {

    /** 状态码：200 成功，400 参数错误，500 服务端错误，其余业务错误码见 {@code BusinessException} */
    private Integer code;

    /** 面向用户的提示信息 */
    private String message;

    /** 响应数据，无数据时为 {@code null} */
    private T data;

    /** 私有构造，统一使用静态工厂方法创建实例 */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ======================== 成功 ========================

    /** 成功 — 无数据（如删除操作、状态变更） */
    public static <T> Result<T> ok() {
        return new Result<>(200, "ok", null);
    }

    /** 成功 — 附带数据（查询类接口最常用） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    /** 成功 — 自定义消息 + 数据（如下单成功、提交成功等） */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ======================== 失败 ========================

    /** 失败 — 默认 500 错误码（通用服务端异常） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /** 失败 — 自定义错误码（如 400 参数校验、10001 库存不足等业务错误） */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
