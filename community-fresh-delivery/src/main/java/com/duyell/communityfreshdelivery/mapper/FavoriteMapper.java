package com.duyell.communityfreshdelivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duyell.communityfreshdelivery.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * <h2>收藏夹 Mapper</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
