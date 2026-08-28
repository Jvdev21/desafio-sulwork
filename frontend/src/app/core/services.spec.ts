import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ColaboradorService } from './colaborador.service';
import { CafeService } from './cafe.service';
import { ParticipacaoService } from './participacao.service';
import { ItemService } from './item.service';

describe('HTTP services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists collaborators from the centralized API URL', () => {
    TestBed.inject(ColaboradorService).list().subscribe((values) => expect(values).toEqual([]));
    const request = http.expectOne('/api/colaboradores');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('creates a cafe', () => {
    TestBed.inject(CafeService).create({ data: '2030-01-01' }).subscribe();
    const request = http.expectOne('/api/cafes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ data: '2030-01-01' });
    request.flush({ id: 1, data: '2030-01-01', createdAt: '' });
  });

  it('creates a participation with multiple items', () => {
    const payload = { colaboradorId: 2, itens: ['Bolo', 'Suco'] };
    TestBed.inject(ParticipacaoService).create(4, payload).subscribe();
    const request = http.expectOne('/api/cafes/4/participantes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 1, cafeId: 4, colaboradorId: 2, createdAt: '', itens: [] });
  });

  it('updates an item', () => {
    TestBed.inject(ItemService).update(7, 'Pão de queijo').subscribe();
    const request = http.expectOne('/api/itens/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ nome: 'Pão de queijo' });
    request.flush({});
  });

  it('updates an item status without offering pending', () => {
    TestBed.inject(ItemService).updateStatus(7, 'TROUXE').subscribe();
    const request = http.expectOne('/api/itens/7/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'TROUXE' });
    request.flush({});
  });
});
