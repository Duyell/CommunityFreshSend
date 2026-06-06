package com.duyell.communityfreshdelivery.service;

import com.duyell.communityfreshdelivery.dto.CartAddDTO;
import com.duyell.communityfreshdelivery.dto.CartItemVO;

import java.util.List;

/**
 * <h2>购物车服务</h2>
 *
 * <p>基于 Redis Hash 实现，key={@code cart:user:{userId}}，field=skuId，value=数量.
 * 不扣库存，仅校验库存是否充足.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
public interface CartService {

    /**
     * 加购（已存在则累加数量）.
     *
     * @param dto SKU ID + 数量
     */
    void add(CartAddDTO dto);

    /**
     * 修改某 SKU 的数量（直接覆盖）.
     *
     * @param dto SKU ID + 新数量
     */
    void updateQty(CartAddDTO dto);

    /**
     * 删除购物车中某个 SKU.
     *
     * @param skuId SKU ID
     */
    void remove(Long skuId);

    /**
     * 清空当前用户购物车.
     */
    void clear();

    /**
     * 查看购物车列表（含商品名/规格/价格/库存状态）.
     *
     * @return 购物车明细
     */
    List<CartItemVO> list();
}
