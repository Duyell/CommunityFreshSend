package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * <h2>收货地址保存请求</h2>
 *
 * <p>新增和编辑共用此 DTO，编辑时前端传完整字段.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Data
public class AddressSaveDTO {

    /** 收货人姓名 */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 20, message = "收货人姓名最长20个字符")
    private String contact;

    /** 收货人电话 */
    @NotBlank(message = "收货人电话不能为空")
    @Size(max = 20, message = "收货人电话最长20个字符")
    private String phone;

    /** 省份 */
    @NotBlank(message = "省份不能为空")
    @Size(max = 20, message = "省份最长20个字符")
    private String province;

    /** 城市 */
    @NotBlank(message = "城市不能为空")
    @Size(max = 20, message = "城市最长20个字符")
    private String city;

    /** 区/县 */
    @NotBlank(message = "区/县不能为空")
    @Size(max = 20, message = "区/县最长20个字符")
    private String district;

    /** 详细地址（门牌号） */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址最长255个字符")
    private String detail;
}
