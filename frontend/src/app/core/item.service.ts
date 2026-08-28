import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ItemCafe, ItemStatus } from './models';

@Injectable({ providedIn: 'root' })
export class ItemService {
  private readonly http = inject(HttpClient);

  create(participacaoId: number, nome: string): Observable<ItemCafe> {
    return this.http.post<ItemCafe>(`${environment.apiUrl}/participacoes/${participacaoId}/itens`, { nome });
  }
  update(id: number, nome: string): Observable<ItemCafe> {
    return this.http.put<ItemCafe>(`${environment.apiUrl}/itens/${id}`, { nome });
  }
  updateStatus(id: number, status: Exclude<ItemStatus, 'PENDENTE'>): Observable<ItemCafe> {
    return this.http.patch<ItemCafe>(`${environment.apiUrl}/itens/${id}/status`, { status });
  }
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/itens/${id}`);
  }
}
