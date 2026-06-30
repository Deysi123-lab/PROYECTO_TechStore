import { Injectable, computed, signal } from '@angular/core';
import { CartItem, Producto } from '../models/api.models';

export const IGV_RATE = 0.18;
export const ENVIO_GRATIS_DESDE = 200;
export const COSTO_ENVIO = 12;

const STORAGE_KEY = 'techstore_cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSignal = signal<CartItem[]>(this.loadFromStorage());

  readonly items = this.itemsSignal.asReadonly();
  readonly itemCount = computed(() => this.itemsSignal().reduce((sum, i) => sum + i.cantidad, 0));

  count(): number {
    return this.itemCount();
  }

  getItems(): CartItem[] {
    return [...this.itemsSignal()];
  }

  add(producto: Producto, cantidad = 1): void {
    const items = [...this.itemsSignal()];
    const existing = items.find((i) => i.producto.id === producto.id);
    const max = producto.stock ?? 99;
    if (existing) {
      existing.cantidad = Math.min(existing.cantidad + cantidad, max);
    } else {
      items.push({ producto, cantidad: Math.min(cantidad, max) });
    }
    this.persist(items);
  }

  setQuantity(productoId: number, cantidad: number): void {
    const items = this.itemsSignal()
      .map((i) => {
        if (i.producto.id !== productoId) return i;
        const max = i.producto.stock ?? 99;
        return { ...i, cantidad: Math.max(1, Math.min(cantidad, max)) };
      })
      .filter((i) => i.cantidad > 0);
    this.persist(items);
  }

  remove(productoId: number): void {
    this.persist(this.itemsSignal().filter((i) => i.producto.id !== productoId));
  }

  clear(): void {
    this.persist([]);
  }

  subtotal(): number {
    return this.itemsSignal().reduce((sum, i) => sum + Number(i.producto.precio) * i.cantidad, 0);
  }

  impuestos(): number {
    return this.subtotal() * IGV_RATE;
  }

  costoEnvio(): number {
    return this.subtotal() >= ENVIO_GRATIS_DESDE ? 0 : COSTO_ENVIO;
  }

  total(): number {
    return this.subtotal() + this.impuestos() + this.costoEnvio();
  }

  private persist(items: CartItem[]): void {
    this.itemsSignal.set(items);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }

  private loadFromStorage(): CartItem[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw) as CartItem[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}
