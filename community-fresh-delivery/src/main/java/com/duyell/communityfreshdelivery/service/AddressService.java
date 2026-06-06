package com.duyell.communityfreshdelivery.service;

import com.duyell.communityfreshdelivery.dto.AddressSaveDTO;
import com.duyell.communityfreshdelivery.dto.AddressVO;

import java.util.List;

/**
 * <h2>收货地址服务</h2>
 *
 * @author duyell
 * @since 2026-06-06
 */
public interface AddressService {

    /**
     * 当前用户的地址列表.
     *
     * @return 地址列表，默认地址排最前
     */
    List<AddressVO> list();

    /**
     * 新增地址，最多 10 个.
     *
     * @param dto 地址信息
     * @return 创建后的地址
     */
    AddressVO create(AddressSaveDTO dto);

    /**
     * 编辑地址（仅限自己的地址）.
     *
     * @param id  地址 ID
     * @param dto 新的地址信息
     * @return 更新后的地址
     */
    AddressVO update(Long id, AddressSaveDTO dto);

    /**
     * 删除地址（逻辑删除，仅限自己的地址）.
     *
     * @param id 地址 ID
     */
    void delete(Long id);

    /**
     * 设为默认地址（取消原有默认，仅限自己的地址）.
     *
     * @param id 地址 ID
     */
    void setDefault(Long id);
}
