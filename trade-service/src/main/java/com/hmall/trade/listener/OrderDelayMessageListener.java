package com.hmall.trade.listener;


import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class OrderDelayMessageListener {


    @Resource
    private IOrderService orderService;

    @Resource
    private PayClient payClient;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.delay_Order_Queue_Name),
            exchange = @Exchange(name = MQConstants.delay_Exchange_Name, delayed = "true"),
            key = MQConstants.delay_Order_Routing_Key
    ))
    public void listenOrderDelayMessage(Long orderId){
        //1. 查询订单
        Order order = orderService.getById(orderId);
        //2. 判断订单状态,判断是否已支付
        if(order == null || order.getStatus() != 1){
            return;
        }
        //3.未支付，查询支付流水状态
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        //4.支付流水状态为已支付，标记订单状态为已支付
        if(payOrderDTO != null && payOrderDTO.getStatus() == 3){
            orderService.markOrderPaySuccess(orderId);
        }else {
            //5.支付流水状态为未支付，关闭订单
            orderService.closeOrder(orderId);
        }

    }
}
