import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Participacao, ParticipacaoRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ParticipacaoService {
  private readonly http = inject(HttpClient);
  private readonly cafesUrl = `${environment.apiUrl}/cafes`;

  list(cafeId: number): Observable<Participacao[]> {
    return this.http.get<Participacao[]>(`${this.cafesUrl}/${cafeId}/participantes`);
  }
  create(cafeId: number, request: ParticipacaoRequest): Observable<Participacao> {
    return this.http.post<Participacao>(`${this.cafesUrl}/${cafeId}/participantes`, request);
  }
  delete(cafeId: number, participacaoId: number): Observable<void> {
    return this.http.delete<void>(`${this.cafesUrl}/${cafeId}/participantes/${participacaoId}`);
  }
}
