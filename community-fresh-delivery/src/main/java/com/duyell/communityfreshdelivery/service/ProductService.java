package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.ProductSaveDTO;
import com.duyell.communityfreshdelivery.dto.ProductVO;

/**
 * <h2>商品服务</h2>
 *
 * @author duyell
 * @since 2026-06-04
 */
public interface ProductService {

    /**
     * 创建商品（含 SKU 列表），主表和子表在同一事务.
     *
     * @param dto 商品信息 + SKU 列表
     * @return 创建后的商品详情
     */
    ProductVO create(ProductSaveDTO dto);

    /**
     * 编辑商品，SKU 采用"先删后插"策略.
     *
     * @param id  商品 ID
     * @param dto 新的商品信息 + 完整 SKU 列表
     * @return 更新后的商品详情
     */
    ProductVO update(Long id, ProductSaveDTO dto);

    /**
     * 更新商品状态（上架/下架/售罄）.
     *
     * @param id     商品 ID
     * @param status 新状态：1=上架 2=下架 3=售罄
     */
    void updateStatus(Long id, Integer status);

    /**
     * 删除商品（逻辑删除，MyBatis-Plus @TableLogic 自动处理）.
     *
     * @param id 商品 ID
     */
    void delete(Long id);

    /**
     * 商品详情（含 SKU 列表）.
     *
     * @param id 商品 ID
     * @return 商品详情，不存在时返回 null
     */
    ProductVO getById(Long id);

    /**
     * 商家端分页列表（不含 SKU 明细）.
     *
     * @param page       页码
     * @param size       每页条数
     * @param categoryId 分类 ID 筛选（可选）
     * @param keyword    关键词模糊搜索（可选）
     * @return 分页结果
     */
    Page<ProductVO> page(int page, int size, Long categoryId, String keyword);

    /**
     * 用户端商品查询（仅上架商品）.
     *
     * <p>支持分类浏览 + 关键词搜索，按价格/时间排序.
     * 价格排序时先全量查上架商品，内存计算最低售价后再分页；
     * 商品总量有限（社区规模），性能无影响.</p>
     *
     * @param page       页码
     * @param size       每页条数
     * @param categoryId 分类 ID 筛选（可选，null=全部分类）
     * @param keyword    关键词模糊搜索（可选，null=不限）
     * @param sort       排序方式：{@code time}=最新（默认）、{@code price_asc}=价格升序、{@code price_desc}=价格降序
     * @return 分页结果
     */
    Page<ProductVO> listForUser(int page, int size, Long categoryId, String keyword, String sort);
}
