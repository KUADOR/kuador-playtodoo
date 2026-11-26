import { Component, computed, input, output } from '@angular/core';
import { Button } from '../button/button';

/**
 * Pagination of «Identidad visual» v3: «Anterior» and «Siguiente» with «Mostrando 1–20 de 54».
 * Hidden when everything fits in one page. `page` is zero-based, like the contract.
 */
@Component({
  selector: 'app-pagination',
  imports: [Button],
  templateUrl: './pagination.html',
  styleUrl: './pagination.css',
})
export class Pagination {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly totalItems = input.required<number>();
  /** Names what is paginated for screen readers, for example «Paginación de cambios de permisos». */
  readonly label = input('Paginación');
  readonly disabled = input(false);

  readonly pageChange = output<number>();

  protected readonly pageCount = computed(() => Math.ceil(this.totalItems() / this.size()));
  protected readonly isFirst = computed(() => this.page() === 0);
  protected readonly isLast = computed(() => this.page() >= this.pageCount() - 1);
  protected readonly summary = computed(() => {
    const first = this.page() * this.size() + 1;
    const last = Math.min(first + this.size() - 1, this.totalItems());
    return `Mostrando ${first}–${last} de ${this.totalItems()}`;
  });
}
