package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

/**
 * <h2>收货地址响应</h2>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Data
@Builder
public class AddressVO {

    /** 地址 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 收货人姓名 */
    private String contact;

    /** 收货人电话 */
    private String phone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址（门牌号） */
    private String detail;

    /** 是否默认地址 */
    private Integer isDefault;
}
