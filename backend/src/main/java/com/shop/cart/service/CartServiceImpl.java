package com.shop.cart.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.cart.dto.CartDto;
import com.shop.cart.dto.CartItemDto;
import com.shop.cart.dto.CartRestoreLine;
import com.shop.cart.dto.CartRestoreResult;
import com.shop.cart.entity.Cart;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartItemMapper;
import com.shop.cart.repository.CartItemRepository;
import com.shop.cart.repository.CartRepository;
import com.shop.product.dto.ProductItemDto;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.map.ProductSearchMapRepository;
import com.shop.search.service.ProductSearchMapService;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private static final String STOCK_OVERFLOW = "STOCK_OVERFLOW";
    private static final String PRODUCT_UNAVAILABLE = "PRODUCT_UNAVAILABLE";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserService userService;
    private final ProductSearchMapService productSearchMapService;
    private final ProductSearchMapRepository productSearchMapRepository;
    private final CartItemMapper cartItemMapper;
    private final CartItemCleanupService cartItemCleanupService;

    // 장바구니 조회(Guest)
    @Override
    @Transactional(readOnly = true)
    public CartDto getCartByGuestId(String guestId) {
        return cartRepository.findByGuestId(guestId)
            .map(cart -> CartDto.builder()
                .id(cart.getId())
                .guestId(cart.getGuestId())
                .cartItems(getCartItemDto(cart))
                .build())
            .orElseGet(() -> CartDto.builder()
                .guestId(guestId)
                .cartItems(Collections.emptyList())
                .build());
    }

    // 장바구니 조회(User)
    @Override
    @Transactional(readOnly = true)
    public CartDto getCartByUserId(String userId) {
        Long userIdLong = userService.findUserIdByPublicId(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return cartRepository.findByUserId(userIdLong)
            .map(cart -> CartDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .cartItems(getCartItemDto(cart))
                .build())
            .orElseGet(() -> CartDto.builder()
                .userId(userIdLong)
                .cartItems(Collections.emptyList())
                .build());
    }

    // 장바구니 추가(Guest)-cart가 없으면 생성하고 추가
    @Override
    @Transactional
    public void addItemToCartForGuest(String guestId, Long searchMapId, Long quantity) {
        Cart cart = cartRepository.findByGuestId(guestId).orElseGet(() -> {
            Cart newCart = Cart.builder()
                .guestId(guestId)
                .build();
            return cartRepository.save(newCart);
        });
        CartItem existingCartItem = cartItemRepository.findByCartAndSearchMapId(cart, searchMapId).orElse(null);
        Long visibleStock = resolveVisibleStockOrRemove(existingCartItem, searchMapId);
        if (existingCartItem != null) {
            assertWithinStock(existingCartItem.getQuantity() + quantity, visibleStock);
            existingCartItem.setQuantity(existingCartItem.getQuantity() + quantity);
            cartItemRepository.save(existingCartItem);
            log.info("cartItem quantity updated: {}", existingCartItem);
        } else {
            assertWithinStock(quantity, visibleStock);
            CartItem cartItem = CartItem.builder()
                .cart(cart)
                .searchMapId(searchMapId)
                .quantity(quantity)
                .build();
            cartItemRepository.save(cartItem);
            log.info("cartItem saved: {}", cartItem);
        }
    }

    // 장바구니 추가(User)-cart가 없으면 생성하고 추가-장바구니에 담겨있는 경우 수량 증가-만일 visibleStock<quantity 이면 증가시키지 않음
    @Override
    @Transactional
    public void addItemToCartForUser(String userId, Long searchMapId, Long quantity) {
        Long userIdLong = userService.findUserIdByPublicId(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUserId(userIdLong).orElseGet(() -> {
            Cart newCart = Cart.builder()
                .userId(userIdLong)
                .build();
            return cartRepository.save(newCart);
        });
        CartItem existingCartItem = cartItemRepository.findByCartAndSearchMapId(cart, searchMapId).orElse(null);
        Long visibleStock = resolveVisibleStockOrRemove(existingCartItem, searchMapId);
        if (existingCartItem != null) {
            assertWithinStock(existingCartItem.getQuantity() + quantity, visibleStock);
            existingCartItem.setQuantity(existingCartItem.getQuantity() + quantity);
            cartItemRepository.save(existingCartItem);
            log.info("cartItem quantity updated: {}", existingCartItem);
        } else {
            assertWithinStock(quantity, visibleStock);
            CartItem cartItem = CartItem.builder()
                .cart(cart)
                .searchMapId(searchMapId)
                .quantity(quantity)
                .build();
            cartItemRepository.save(cartItem);
            log.info("cartItem saved: {}", cartItem);
        }
    }

    // 장바구니 삭제(Guest)
    @Override
    @Transactional
    public void removeItemFromCartForGuest(String guestId, Long searchMapId) {
        Cart cart = cartRepository.findByGuestId(guestId).orElseThrow(() -> new RuntimeException("Cart not found"));
        CartItem cartItem = cartItemRepository.findByCartAndSearchMapId(cart, searchMapId).orElseThrow(() -> new RuntimeException("Cart item not found"));
        cartItemRepository.delete(cartItem);
    }

    // 장바구니 수량 수정(Guest)
    @Override
    @Transactional
    public void updateItemQuantityForGuest(String guestId, Long searchMapId, Long quantity) {
        Cart cart = cartRepository.findByGuestId(guestId).orElseThrow(() -> new RuntimeException("Cart not found"));
        CartItem cartItem = cartItemRepository.findByCartAndSearchMapId(cart, searchMapId).orElseThrow(() -> new RuntimeException("Cart item not found"));
        assertWithinStock(quantity, resolveVisibleStockOrRemove(cartItem, searchMapId));
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    // 장바구니 수량 수정(User)
    @Override
    @Transactional
    public void updateItemQuantityForUser(String userId, Long searchMapId, Long quantity) {
        Long userIdLong = userService.findUserIdByPublicId(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUserId(userIdLong).orElseThrow(() -> new RuntimeException("Cart not found"));
        CartItem cartItem = cartItemRepository.findByCartAndSearchMapId(cart, searchMapId).orElseThrow(() -> new RuntimeException("Cart item not found"));
        assertWithinStock(quantity, resolveVisibleStockOrRemove(cartItem, searchMapId));
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    // 장바구니 삭제(User)
    @Override
    @Transactional
    public void removeItemFromCartForUser(String userId, Long searchMapId) {
        Long userIdLong = userService.findUserIdByPublicId(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUserId(userIdLong).orElseThrow(() -> new RuntimeException("Cart not found"));
        CartItem cartItem = cartItemRepository.findByCartAndSearchMapId(cart, searchMapId).orElseThrow(() -> new RuntimeException("Cart item not found"));
        cartItemRepository.delete(cartItem);
    }

    // 장바구니 비우기(User)
    @Override
    @Transactional
    public void clearCartForUser(String userId) {
        Long userIdLong = userService.findUserIdByPublicId(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUserId(userIdLong).orElseThrow(() -> new RuntimeException("Cart not found"));
        cartItemRepository.deleteByCart(cart);
    }
    // 장바구니 비우기(Guest)
    @Override
    @Transactional
    public void clearCartForGuest(String guestId) {
        Cart cart = cartRepository.findByGuestId(guestId).orElseThrow(() -> new RuntimeException("Cart not found"));
        cartItemRepository.deleteByCart(cart);
    }

    @Override
    @Transactional
    public void discardGuestCart(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return;
        }
        cartRepository.findByGuestId(guestId).ifPresent(cart -> {
            cartRepository.delete(cart);
            log.info("Discarded guest cart on login: guestId={}", guestId);
        });
    }

    /** 로그인 시 병합용. 현재 미사용, 추후 재활성화 가능. */
    @Override
    @Transactional
    public void mergeGuestCartIntoUserCart(String guestId, String userPublicId) {
        if (guestId == null || guestId.isBlank()) {
            return;
        }
        Cart guestCart = cartRepository.findByGuestId(guestId).orElse(null);
        if (guestCart == null) {
            return;
        }
        List<CartItem> guestItems = cartItemRepository.findByCart(guestCart);
        if (guestItems.isEmpty()) {
            cartRepository.delete(guestCart);
            return;
        }

        Long userIdLong = userService.findUserIdByPublicId(userPublicId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart userCart = cartRepository.findByUserId(userIdLong).orElseGet(() -> cartRepository.save(
                Cart.builder().userId(userIdLong).build()));

        for (CartItem guestItem : guestItems) {
            Long searchMapId = guestItem.getSearchMapId();
            Long stock = resolveVisibleStockForMerge(searchMapId);
            if (stock == null) {
                continue;
            }

            CartItem userItem = cartItemRepository.findByCartAndSearchMapId(userCart, searchMapId).orElse(null);
            long memberQty = userItem != null ? userItem.getQuantity() : 0L;
            long finalQty = Math.min(memberQty + guestItem.getQuantity(), stock);

            if (finalQty <= 0) {
                if (userItem != null) {
                    cartItemRepository.delete(userItem);
                }
                continue;
            }

            if (userItem != null) {
                userItem.setQuantity(finalQty);
                cartItemRepository.save(userItem);
            } else {
                cartItemRepository.save(CartItem.builder()
                        .cart(userCart)
                        .searchMapId(searchMapId)
                        .quantity(finalQty)
                        .build());
            }
        }

        cartRepository.delete(guestCart);
        log.info("Merged guest cart into user cart: guestId={}, userPublicId={}", guestId, userPublicId);
    }

    @Override
    @Transactional
    public CartRestoreResult restoreItemsForUserId(Long userId, List<CartRestoreLine> lines) {
        if (userId == null || lines == null || lines.isEmpty()) {
            return CartRestoreResult.builder().build();
        }

        Cart userCart = cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(
                Cart.builder().userId(userId).build()));

        int linesRequested = 0;
        int linesFullyRestored = 0;
        int linesPartiallyRestored = 0;
        int linesSkipped = 0;

        for (CartRestoreLine line : lines) {
            if (line.getSearchMapId() == null || line.getQuantity() <= 0) {
                linesSkipped++;
                continue;
            }

            linesRequested++;
            Long searchMapId = line.getSearchMapId();
            Long stock = resolveVisibleStockForMerge(searchMapId);
            if (stock == null) {
                linesSkipped++;
                continue;
            }

            CartItem userItem = cartItemRepository.findByCartAndSearchMapId(userCart, searchMapId).orElse(null);
            long memberQty = userItem != null ? userItem.getQuantity() : 0L;
            long requestedQty = line.getQuantity();
            long finalQty = Math.min(memberQty + requestedQty, stock);
            long addedQty = finalQty - memberQty;

            if (finalQty <= 0 || addedQty <= 0) {
                linesSkipped++;
                continue;
            }

            if (userItem != null) {
                userItem.setQuantity(finalQty);
                cartItemRepository.save(userItem);
            } else {
                cartItemRepository.save(CartItem.builder()
                        .cart(userCart)
                        .searchMapId(searchMapId)
                        .quantity(finalQty)
                        .build());
            }

            if (addedQty >= requestedQty) {
                linesFullyRestored++;
            } else {
                linesPartiallyRestored++;
            }
        }

        return CartRestoreResult.builder()
                .linesRequested(linesRequested)
                .linesFullyRestored(linesFullyRestored)
                .linesPartiallyRestored(linesPartiallyRestored)
                .linesSkipped(linesSkipped)
                .build();
    }

    /** 담기·수량변경용 재고 조회. 상품을 해석할 수 없으면 장바구니에 남은 줄을 제거하고 PRODUCT_UNAVAILABLE로 알린다. */
    private Long resolveVisibleStockOrRemove(CartItem cartItem, Long searchMapId) {
        try {
            return resolveSaleVisibleStock(searchMapId);
        } catch (ResponseStatusException ex) {
            if (cartItem != null) {
                removeUnavailableItem(cartItem, ex.getReason());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PRODUCT_UNAVAILABLE);
        }
    }

    /** 병합용 재고 조회. 상품 없음 시 null(스킵), null 재고는 0. */
    private Long resolveVisibleStockForMerge(Long searchMapId) {
        try {
            return resolveSaleVisibleStock(searchMapId);
        } catch (ResponseStatusException ex) {
            log.warn("Skipping cart merge line: searchMapId={}, reason={}", searchMapId, ex.getReason());
            return null;
        }
    }

    private Long resolveSaleVisibleStock(Long searchMapId) {
        ProductSearchMap map = productSearchMapRepository.findById(searchMapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        assertCartSupportedProductTable(map);
        ProductItemDto dto = productSearchMapService.getProductItemDtoBySearchMapId(searchMapId);
        Long stock = dto.getCurrentVisibleStock();
        return stock != null ? stock : 0L;
    }

    private static void assertWithinStock(long requestedQuantity, long visibleStock) {
        if (requestedQuantity > visibleStock) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, STOCK_OVERFLOW);
        }
    }

    /** 체크아웃 드래프트와 동일: ProductSearchMap 테이블 기준으로 판별 (CARD_PRODUCT DTO는 table=UNION_PRICE) */
    private static void assertCartSupportedProductTable(ProductSearchMap map) {
        ProductTableEnum table = map.getTableName();
        if (table == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PRODUCT_UNAVAILABLE);
        }
        if (table != ProductTableEnum.CARD_PRODUCT
                && table != ProductTableEnum.SEALED_PRODUCT
                && table != ProductTableEnum.MANUAL_PRODUCT
                && table != ProductTableEnum.SUPPLY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PRODUCT_TABLE");
        }
    }

    // 장바구니 조회(Dto)
    private List<CartItemDto> getCartItemDto(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        List<CartItemDto> cartItemDtos = new ArrayList<>(cartItems.size());
        for (CartItem cartItem : cartItems) {
            CartItemDto cartItemDto = toCartItemDto(cartItem);
            if (cartItemDto != null) {
                cartItemDtos.add(cartItemDto);
            }
        }
        return cartItemDtos;
    }

    /** 장바구니 조회(Dto). 상품을 해석할 수 없는 줄은 장바구니에서 제거하고 null을 반환한다. */
    private CartItemDto toCartItemDto(CartItem cartItem) {
        ProductItemDto productItemDto;
        try {
            productItemDto = productSearchMapService.getProductItemDtoBySearchMapId(cartItem.getSearchMapId());
        } catch (ResponseStatusException ex) {
            removeUnavailableItem(cartItem, ex.getReason());
            return null;
        }
        CartItemDto cartItemDto = cartItemMapper.from(cartItem, productItemDto);
        if (cartItemDto == null) {
            removeUnavailableItem(cartItem, "MAPPING_FAILED");
        }
        return cartItemDto;
    }

    private void removeUnavailableItem(CartItem cartItem, String reason) {
        cartItemCleanupService.removeUnavailableItem(cartItem.getId(), cartItem.getSearchMapId(), reason);
    }
}
