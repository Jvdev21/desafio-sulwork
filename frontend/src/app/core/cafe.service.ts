import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Cafe, CafeRequest } from './models';

@Injectable({ providedIn: 'root' })
export class CafeService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cafes`;

  list(): Observable<Cafe[]> { return this.http.get<Cafe[]>(this.url); }
  get(id: number): Observable<Cafe> { return this.http.get<Cafe>(`${this.url}/${id}`); }
  create(request: CafeRequest): Observable<Cafe> { return this.http.post<Cafe>(this.url, request); }
  update(id: number, request: CafeRequest): Observable<Cafe> { return this.http.put<Cafe>(`${this.url}/${id}`, request); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
