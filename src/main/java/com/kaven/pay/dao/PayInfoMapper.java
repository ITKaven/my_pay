package com.kaven.pay.dao;

import com.kaven.pay.pojo.PayInfo;
import org.springframework.stereotype.Component;

@Component
public interface PayInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PayInfo record);

    int insertSelective(PayInfo record);

    PayInfo selectByPrimaryKey(Integer id);

    PayInfo selectByOrderNo(Long orderNo);

    int updateByPrimaryKeySelective(PayInfo record);

    int updateByPrimaryKey(PayInfo record);
}