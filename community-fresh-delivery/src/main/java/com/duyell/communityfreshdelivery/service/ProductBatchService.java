package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.ProductBatchSaveDTO;
import com.duyell.communityfreshdelivery.dto.ProductBatchVO;

import java.util.List;
import java.util.Map;

/**
 * <h2>批次库存服务</h2>
 *
 * <p>商家端入库 + FIFO 分配 + 临期/低库存预警.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface ProductBatchService {

    /**
     * 批次入库。创建批次记录，库存由 product_batch.remaining 管理.
     *
     * @param dto 入库信息
     * @return 创建后的批次
     */
    ProductBatchVO create(ProductBatchSaveDTO dto);

    /**
     * 按商品查批次列表.
     *
     * @param productId  商品ID
     * @param nearExpiry 临期筛选（null=全部 1=仅临期）
     * @param page       页码
     * @param size       每页条数
     * @return 分页结果
     */
    Page<ProductBatchVO> pageByProduct(Long productId, Integer nearExpiry, int page, int size);

    /**
     * 获取临期商品批次列表.
     *
     * @return 临期批次列表
     */
    List<ProductBatchVO> getNearExpiry();

    /**
     * FIFO 分配库存（下单时自动调用，分拣指导）.
     * 按过期日升序分配，早过期的先出.
     *
     * @param productId    商品ID
     * @param needQuantity 需要分配的数量
     * @return 分配结果列表，每项含 batchId + batchNo + allocatedQuantity + expiryDate
     */
    List<Map<String, Object>> allocateFIFO(Long productId, int needQuantity);
}
