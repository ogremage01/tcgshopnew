import { create } from "zustand";
import { persist } from "zustand/middleware";

export interface CartItem {
  productId: string;
  name: string;
  price: number;
  quantity: number;
  imageUrl?: string;
}

interface CartStore {
  items: CartItem[];
  addItem: (item: Omit<CartItem, "quantity"> & { quantity?: number }) => void;
  removeItem: (productId: string) => void;
  updateQuantity: (productId: string, quantity: number) => void;
  clearCart: () => void;
  itemCount: number;
}

export const useCartStore = create<CartStore>()(
  persist(
    (set) => ({
      items: [],
      itemCount: 0,
      addItem: (item) =>
        set((state) => {
          const existing = state.items.find((i) => i.productId === item.productId);
          const newItems = existing
            ? state.items.map((i) =>
                i.productId === item.productId
                  ? { ...i, quantity: i.quantity + (item.quantity ?? 1) }
                  : i
              )
            : [...state.items, { ...item, quantity: item.quantity ?? 1 }];
          return {
            items: newItems,
            itemCount: newItems.reduce((acc, i) => acc + i.quantity, 0),
          };
        }),
      removeItem: (productId) =>
        set((state) => {
          const newItems = state.items.filter((i) => i.productId !== productId);
          return {
            items: newItems,
            itemCount: newItems.reduce((acc, i) => acc + i.quantity, 0),
          };
        }),
      updateQuantity: (productId, quantity) =>
        set((state) => {
          if (quantity <= 0) {
            const newItems = state.items.filter((i) => i.productId !== productId);
            return { items: newItems, itemCount: newItems.reduce((a, i) => a + i.quantity, 0) };
          }
          const newItems = state.items.map((i) =>
            i.productId === productId ? { ...i, quantity } : i
          );
          return {
            items: newItems,
            itemCount: newItems.reduce((a, i) => a + i.quantity, 0),
          };
        }),
      clearCart: () => set({ items: [], itemCount: 0 }),
    }),
    { name: "cart-storage" }
  )
);
