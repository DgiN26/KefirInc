package com.example.backet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @PostMapping("/client/{clientId}")
    public Cart createCart(@PathVariable int clientId) {
        Cart activeCart = cartRepository.findByClientIdAndStatus(clientId, "active");
        if (activeCart != null) {
            return activeCart;
        }
        Cart cart = new Cart(clientId, "active");
        return cartRepository.save(cart);
    }

    @PostMapping("/{cartId}/add")
    public CartItem addToCart(@PathVariable int cartId,
                              @RequestParam int productId,
                              @RequestParam int quantity,
                              @RequestParam double price) {
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            return cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem(cartId, productId, quantity, price);
            return cartItemRepository.save(newItem);
        }
    }

    @GetMapping("/client/{clientId}")
    public List<Cart> getClientCarts(@PathVariable int clientId) {
        return cartRepository.findByClientId(clientId);
    }
}
