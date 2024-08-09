package com.beyond.ordersystem.ordering.Service;


import com.beyond.ordersystem.common.serivce.StockInventoryService;


import com.beyond.ordersystem.ordering.Domain.OrderStatus;
import com.beyond.ordersystem.ordering.Domain.Ordering;
import com.beyond.ordersystem.ordering.Dto.OrderListResDto;
import com.beyond.ordersystem.ordering.Dto.OrderSaveReqDto;
import com.beyond.ordersystem.ordering.Repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.Repository.OrderingRepository;

import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.*;


@Service
@Transactional  // 붙어서 더티체킹 ㅇㅇ
public class OrderingService {

    private final OrderingRepository orderingRepository;

    private final OrderDetailRepository orderDetailRepository;
    private final StockInventoryService stockInventoryService;
    private final StockDecreaseEventHandler stockDecreaseEventHandler;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, OrderDetailRepository orderDetailRepository, StockInventoryService stockInventoryService, StockDecreaseEventHandler stockDecreaseEventHandler) {
        this.orderingRepository = orderingRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.stockInventoryService = stockInventoryService;
        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
    }


    @Synchronized
    public Ordering orderCreate(List<OrderSaveReqDto> dtos) {

//       Synchronized 설정한다 하더라도, 재고 감소가 db에 반영되는 시점은 트랜잭션이 커밋되고 종료되는 시점
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Ordering ordering = Ordering.builder()
                .memberEmail(memberEmail)
                .build();

//        나중에 하기 위해 주석
//        for (OrderSaveReqDto dto : dtos) {
//            int quantity = dto.getProductCount();
//            // Product API에 요청을 통해 product객체를 조회해야 함
//            if(product.getName().contains("sale")){
//                //redis를 통한 재고관리 및 재고잔량 확인
//
//                int newQuantity = stockInventoryService.decreaseStock(dto.getProductId(), dto.getProductCount()).intValue();
//                if(newQuantity < 0){
//                    throw new IllegalArgumentException("재고 부족");
//                }
////                rdb에 재고를 업데이트. rabbitmq를 통해 비동기적으로 이벤트 처리
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(), dto.getProductCount()));
//            }else{
//                if(product.getStockQuantity() < quantity){
//                    throw new IllegalArgumentException("재고 부족");
//                }
//            }
//
//            // 변경 감지(더티체킹)으로 인해 별도의 save 불필요
//            product.updateStockQuantity(quantity);
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//
//            ordering.getOrderDetails().add(orderDetail);
//        }

        Ordering savedOrdering = orderingRepository.save(ordering);
        return savedOrdering;
    }

    public List<OrderListResDto> listorder() {
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos = new ArrayList<>();

        for (Ordering order : orderings) {
            orderListResDtos.add(order.fromEntity());
        }
        return orderListResDtos;
    }


    public List<OrderListResDto> myOrders(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Ordering> orderings = orderingRepository.findByMemberEmail(email);
        List<OrderListResDto> orderListResDtos = new ArrayList<>();
        for (Ordering order : orderings) {
            orderListResDtos.add(order.fromEntity());
        }
        return orderListResDtos;
    }

    public Ordering orderCancle(Long id){
        Ordering ordering = orderingRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException("not found order"));
        ordering.updateStatus(OrderStatus.CANCELLED);
        return ordering;
    }
}

