package com.kaven.pay.service.impl;

import com.kaven.pay.dao.PayInfoMapper;
import com.kaven.pay.enums.PayPlatformEnum;
import com.kaven.pay.pojo.PayInfo;
import com.kaven.pay.service.IPayService;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private PayInfoMapper payInfoMapper;

    @Override
    public PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum) {
        //写入数据库
        PayInfo payInfo = new PayInfo(Long.parseLong(orderId) ,
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(),
                OrderStatusEnum.NOTPAY.name(),
                amount);
        payInfoMapper.insertSelective(payInfo);

        PayRequest payRequest = new PayRequest();
        payRequest.setOrderName("4974091-基于个性化推荐的网上购物系统的设计与实现");
        payRequest.setOrderId(orderId);
        payRequest.setOrderAmount(amount.doubleValue());
        payRequest.setPayTypeEnum(bestPayTypeEnum);

        PayResponse payResponse = bestPayService.pay(payRequest);
        log.info("发起支付 payResponse={}",payResponse);

        return payResponse;
    }

    @Override
    public String asyncNotify(String notifyData) {
        //1.签名检验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("异步通知 payResponse={}",payResponse);

        //2.金额检验（从数据库查订单）
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(payResponse.getOrderId()));
        if(payInfo == null){
            throw new RuntimeException("通过orderId查询为空！");
        }
        if(!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS.name())){
            if(payInfo.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0){
                throw new RuntimeException("异步通知中的金额和数据库里不一致! orderNo="+payResponse.getOrderId());
            }
            //3.修改订单支付状态
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            payInfo.setPlatformNumber(payResponse.getOutTradeNo());
//            payInfo.setUpdateTime(null);
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        //TODO pay发送MQ消息，mall接受MQ消息

        /**
         * 4.告诉微信不要再通知了
         *   告诉支付宝不要再通知了
         * */
        if(payResponse.getPayPlatformEnum() == BestPayPlatformEnum.WX){
            return  "<xml>\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
        }
        else if(payResponse.getPayPlatformEnum() == BestPayPlatformEnum.ALIPAY){
            return "success";
        }
        throw new RuntimeException("异步通知中错误的支付平台");
    }

    @Override
    public PayInfo queryByOrderId(String orderId) {
        return payInfoMapper.selectByOrderNo(Long.parseLong(orderId));
    }
}
