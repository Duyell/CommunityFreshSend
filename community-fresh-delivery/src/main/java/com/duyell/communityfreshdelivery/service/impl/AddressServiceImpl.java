package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.AddressSaveDTO;
import com.duyell.communityfreshdelivery.dto.AddressVO;
import com.duyell.communityfreshdelivery.entity.Address;
import com.duyell.communityfreshdelivery.mapper.AddressMapper;
import com.duyell.communityfreshdelivery.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * <h2>收货地址服务实现</h2>
 *
 * <h3>关键设计</h3>
 * <ul>
 *   <li><b>用户隔离</b> — 所有操作从 SecurityContext 取当前用户 ID，仅操作自己的地址</li>
 *   <li><b>默认地址</b> — 设为默认时取消用户原有默认（一般只有一个）</li>
 *   <li><b>数量限制</b> — 每个用户最多 10 个地址</li>
 *   <li><b>排序</b> — 默认地址排最前</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    private static final int MAX_ADDRESS_COUNT = 10;

    // SecurityUtil.currentUserId() → 改用 SecurityUtil.SecurityUtil.currentUserId()，消除重复代码

    @Override
    public List<AddressVO> list() {
        Long userId = SecurityUtil.currentUserId();
        List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getCreateTime)
        );
        return addresses.stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(AddressSaveDTO dto) {
        Long userId = SecurityUtil.currentUserId();

        long count = addressMapper.selectCount(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
        );
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(30001, "最多添加10个收货地址");
        }

        Address address = new Address();
        address.setUserId(userId);
        fillFields(address, dto);

        if (count == 0) {
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }

        addressMapper.insert(address);
        log.info("地址创建成功: id={}, userId={}, contact={}", address.getId(), userId, dto.getContact());
        return toVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long id, AddressSaveDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        Address address = getOwnAddress(id, userId);

        fillFields(address, dto);
        addressMapper.updateById(address);
        log.info("地址编辑成功: id={}, userId={}", id, userId);
        return toVO(address);
    }

    @Override
    public void delete(Long id) {
        Long userId = SecurityUtil.currentUserId();
        getOwnAddress(id, userId);
        addressMapper.deleteById(id);
        log.info("地址删除成功: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Long userId = SecurityUtil.currentUserId();
        getOwnAddress(id, userId);

        // 取消用户原有默认地址
        addressMapper.update(null,
                new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .eq(Address::getIsDefault, 1)
                        .set(Address::getIsDefault, 0)
        );

        // 设置新默认
        Address address = new Address();
        address.setId(id);
        address.setIsDefault(1);
        addressMapper.updateById(address);
        log.info("默认地址已更新: id={}, userId={}", id, userId);
    }

    /** 查询地址，并校验归属（非本人地址拒绝操作） */
    private Address getOwnAddress(Long id, Long userId) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(30002, "地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(30003, "无权操作该地址");
        }
        return address;
    }

    /** Entity → VO */
    private AddressVO toVO(Address a) {
        return AddressVO.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .contact(a.getContact())
                .phone(a.getPhone())
                .province(a.getProvince())
                .city(a.getCity())
                .district(a.getDistrict())
                .detail(a.getDetail())
                .isDefault(a.getIsDefault())
                .build();
    }

    /** DTO 字段填充到 Entity（create 和 update 共用） */
    private void fillFields(Address address, AddressSaveDTO dto) {
        address.setContact(dto.getContact());
        address.setPhone(dto.getPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetail(dto.getDetail());
    }
}
