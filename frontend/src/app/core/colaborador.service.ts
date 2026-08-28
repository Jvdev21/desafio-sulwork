import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Colaborador, ColaboradorRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ColaboradorService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/colaboradores`;

  list(): Observable<Colaborador[]> { return this.http.get<Colaborador[]>(this.url); }
  get(id: number): Observable<Colaborador> { return this.http.get<Colaborador>(`${this.url}/${id}`); }
  create(request: ColaboradorRequest): Observable<Colaborador> { return this.http.post<Colaborador>(this.url, request); }
  update(id: number, request: ColaboradorRequest): Observable<Colaborador> { return this.http.put<Colaborador>(`${this.url}/${id}`, request); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
