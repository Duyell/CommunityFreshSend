package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.LoginDTO;
import com.duyell.communityfreshdelivery.dto.RegisterDTO;
import com.duyell.communityfreshdelivery.service.AuthService;
import com.duyell.communityfreshdelivery.dto.LoginVO;
import com.duyell.communityfreshdelivery.dto.RegisterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>认证接口</h2>
 *
 * <p>提供登录/注册/登出等公开接口，不拦截（由 {@code SecurityConfig} 放行）.</p>
 *
 * @author duyell
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录/注册/登出")
public class AuthController {

    private final AuthService authService;

    /**
     * 手机号 + 密码登录.
     *
     * <p>成功返回 JWT token 和用户基本信息，前端存 token 到 localStorage，
     * 后续请求带 {@code Authorization: Bearer <token>}.</p>
     *
     * @param dto 手机号 + 密码
     * @return 登录成功 → token + 用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.ok("登录成功", vo);
    }

    /**
     * 新用户注册.
     *
     * <p>注册成功后自动登录，直接返回 JWT 令牌，无需再调登录接口.</p>
     *
     * @param dto 手机号 + 密码 + 昵称
     * @return 注册成功 → token + 用户信息
     */
    @PostMapping("/register")
    @Operation(summary = "注册")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterDTO dto) {
        RegisterVO vo = authService.register(dto);
        return Result.ok("注册成功", vo);
    }
}
