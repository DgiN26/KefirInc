package com.example.ApiGateWay;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "cart-service", url = "http://localhost:8085")
public interface CartServiceClient {

    @PostMapping("/api/cart/client/{clientId}")
    Object createCart(@PathVariable int clientId);

    @PostMapping("/api/cart/{cartId}/add")
    Object addToCart(@PathVariable int cartId,
                     @RequestParam int productId,
                     @RequestParam int quantity,
                     @RequestParam double price);

    @GetMapping("/api/cart/client/{clientId}")
    List<Object> getClientCarts(@PathVariable int clientId);
}
