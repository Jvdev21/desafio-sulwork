import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage } from '../../core/api-error';
import { CafeService } from '../../core/cafe.service';
import { ColaboradorService } from '../../core/colaborador.service';
import { formatCpf } from '../../core/cpf-utils';
import { formatDate, formatDateLong, todayIso } from '../../core/date-utils';
import { ItemService } from '../../core/item.service';
import { Cafe, Colaborador, ItemCafe, ItemStatus, Participacao } from '../../core/models';
import { ParticipacaoService } from '../../core/participacao.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { ItemDialogComponent } from './item-dialog.component';
import { ParticipacaoDialogComponent } from './participacao-dialog.component';

@Component({
  selector: 'app-cafe-detail',
  standalone: true,
  imports: [RouterLink, MatButton, MatIconButton, MatCard, MatCardActions, MatCardContent, MatChipsModule, MatIcon, MatProgressSpinner],
  template: `
    @if (loading()) {
      <div class="loading"><mat-spinner diameter="42" /></div>
    } @else if (cafe()) {
      <div class="detail-top"><a routerLink="/cafes" class="back-link"><mat-icon>arrow_back</mat-icon></a><span>Cafés da manhã</span><mat-icon>chevron_right</mat-icon><strong>{{ date(cafe()!.data) }}</strong>@if (isFuture() && availableCollaborators().length) { <button mat-flat-button (click)="addParticipant()"><mat-icon>add</mat-icon> Adicionar participante</button> }</div>

      <section class="cafe-summary">
        <span class="date-symbol"><mat-icon>event_available</mat-icon></span>
        <span class="date-copy"><small>{{ weekday(cafe()!.data) }}</small><strong>{{ date(cafe()!.data) }}</strong></span>
        <span class="summary-divider"></span>
        <span class="meeting-copy"><h1>Café da manhã</h1><p>Encontro organizado pela equipe</p></span>
        <span class="phase-copy"><mat-chip [class]="'phase-' + phaseKey()">{{ phaseLabel() }}</mat-chip><small>{{ longDate(cafe()!.data) }}</small></span>
        @if (isFuture()) { <a mat-icon-button [routerLink]="['/cafes', cafe()!.id, 'editar']" aria-label="Editar data"><mat-icon>edit_calendar</mat-icon></a> }
      </section>

      @if (isFuture() && !colaboradores().length) {
        <div class="guidance"><span><mat-icon>info</mat-icon><span><strong>Cadastre um colaborador primeiro</strong><small>É necessário ter alguém cadastrado antes de adicionar participantes.</small></span></span><a mat-button routerLink="/colaboradores/novo">Cadastrar colaborador <mat-icon>arrow_forward</mat-icon></a></div>
      }

      <div class="detail-layout">
        <section class="participants-panel">
          <div class="panel-tabs"><strong>Participantes</strong><span>{{ participacoes().length }}</span></div>
          @if (!participacoes().length) {
            <div class="empty-state"><span class="empty-icon"><mat-icon>group_off</mat-icon></span><h2>Nenhum participante adicionado ainda.</h2><p>Escolha um colaborador e informe o que ele levará para este café.</p>@if (isFuture() && availableCollaborators().length) { <button mat-flat-button (click)="addParticipant()" data-cy="primeiro-participante"><mat-icon>person_add</mat-icon> Adicionar primeiro participante</button> }</div>
          } @else {
            <div class="participant-list">
              @for (participation of participacoes(); track participation.id) {
                <mat-card appearance="outlined" class="participant-row interactive-card">
                  <mat-card-content>
                    <div class="person-column"><span class="avatar">{{ initials(collaborator(participation.colaboradorId)?.nome) }}</span><span><strong>{{ collaborator(participation.colaboradorId)?.nome ?? 'Colaborador #' + participation.colaboradorId }}</strong><small>CPF {{ cpf(collaborator(participation.colaboradorId)?.cpf) }}</small></span></div>
                    <div class="items-column"><small>Itens que irá trazer</small>
                      @if (!participation.itens.length) { <span class="no-items">Nenhum item cadastrado.</span> }
                      @for (item of participation.itens; track item.id) {
                        <div class="item-line">
                          <span class="item-name"><span class="item-emoji" aria-hidden="true">{{ itemIcon(item.nome) }}</span>{{ item.nome }}</span>
                          <mat-chip [class]="'status-' + item.status.toLowerCase()">{{ statusLabel(item.status) }}</mat-chip>
                          @if (isToday()) {
                            <span class="status-actions" aria-label="Alterar status de {{ item.nome }}"><button mat-icon-button type="button" [class.selected]="item.status === 'TROUXE'" [disabled]="updatingStatus() === item.id" (click)="changeStatus(item, 'TROUXE')" aria-label="Marcar como trouxe"><mat-icon>check</mat-icon></button><button mat-icon-button type="button" class="not-brought" [class.selected]="item.status === 'NAO_TROUXE'" [disabled]="updatingStatus() === item.id" (click)="changeStatus(item, 'NAO_TROUXE')" aria-label="Marcar como não trouxe"><mat-icon>close</mat-icon></button></span>
                          }
                          @if (isFuture()) { <span class="item-actions"><button mat-icon-button (click)="editItem(item)" aria-label="Editar item"><mat-icon>edit</mat-icon></button><button mat-icon-button (click)="deleteItem(item)" aria-label="Excluir item"><mat-icon>delete</mat-icon></button></span> }
                        </div>
                      }
                    </div>
                  </mat-card-content>
                  @if (isFuture()) { <mat-card-actions><button mat-button (click)="addItem(participation)"><mat-icon>add</mat-icon> Adicionar item</button><button mat-button class="danger-action" (click)="removeParticipant(participation)"><mat-icon>person_remove</mat-icon> Remover</button></mat-card-actions> }
                </mat-card>
              }
            </div>
          }
        </section>

        <aside class="detail-aside">
          <mat-card appearance="outlined" class="resume-card">
            <mat-card-content>
              <h2><mat-icon>free_breakfast</mat-icon> Resumo do café</h2>
              <span class="participant-total"><span><small>Participantes</small><strong>{{ participacoes().length }}</strong></span><span><mat-icon>groups</mat-icon></span></span>
              <div class="status-resume"><h3>Status dos itens</h3><span class="resume-success"><i><mat-icon>check</mat-icon></i>Trouxe<strong>{{ statusCount('TROUXE') }}</strong></span><span class="resume-warning"><i><mat-icon>schedule</mat-icon></i>Pendente<strong>{{ statusCount('PENDENTE') }}</strong></span><span class="resume-danger"><i><mat-icon>close</mat-icon></i>Não trouxe<strong>{{ statusCount('NAO_TROUXE') }}</strong></span></div>
            </mat-card-content>
          </mat-card>
          <div class="tip-card"><mat-icon>lightbulb</mat-icon><span><strong>Dica</strong><small>@if (isToday()) { Atualize os status conforme os colaboradores entregarem seus itens. } @else if (isFuture()) { Organize os itens antes da data do café. } @else { Consulte o resultado final deste encontro. }</small></span></div>
        </aside>
      </div>
    }
  `,
  styles: [`
    .detail-top { display: flex; align-items: center; gap: 8px; min-height: 44px; margin-bottom: 16px; color: var(--text-secondary); font-size: .82rem; }.detail-top > button { margin-left: auto; }.detail-top > mat-icon { width: 17px; height: 17px; font-size: 17px; }.detail-top strong { color: var(--text-primary); }
    .back-link { display: grid; place-items: center; width: 40px; height: 40px; border: 1px solid var(--border); border-radius: 10px; color: var(--mv-blue); text-decoration: none; background: #fff; }.back-link mat-icon { width: 19px; height: 19px; font-size: 19px; }
    .cafe-summary { display: grid; grid-template-columns: auto auto auto 1fr auto auto; align-items: center; gap: 22px; min-height: 156px; margin-bottom: 22px; padding: 24px; border: 1px solid var(--border); border-radius: var(--radius-md); background: #fff; box-shadow: var(--shadow-sm); animation: card-in 350ms ease both; }
    .date-symbol { display: grid; place-items: center; width: 66px; height: 66px; border-radius: 50%; color: #c77921; background: #fff1df; }.date-symbol mat-icon { width: 33px; height: 33px; font-size: 33px; }.date-copy { display: grid; gap: 5px; min-width: 150px; }.date-copy small { color: var(--text-secondary); font-size: .72rem; font-weight: 800; text-transform: uppercase; }.date-copy strong { color: var(--mv-blue); font-size: 1.75rem; }.summary-divider { width: 1px; height: 78px; background: var(--border); }
    .meeting-copy h1 { margin: 0 0 7px; font-size: 1.55rem; }.meeting-copy p { margin: 0; color: var(--text-secondary); }.phase-copy { display: grid; justify-items: start; gap: 9px; }.phase-copy small { color: var(--text-secondary); font-size: .76rem; }
    .phase-future { --mdc-chip-elevated-container-color: #e4f3ec; color: #16745d; }.phase-today { --mdc-chip-elevated-container-color: #fff0df; color: #b96917; }.phase-past { --mdc-chip-elevated-container-color: #eaf0f7; color: var(--mv-blue); }
    .guidance { display: flex; justify-content: space-between; align-items: center; gap: 18px; margin-bottom: 20px; padding: 15px 18px; border: 1px solid #c9e5e0; border-radius: 14px; background: #eff9f7; }.guidance > span { display: flex; align-items: center; gap: 12px; }.guidance > span > mat-icon { color: var(--mv-teal); }.guidance span span { display: grid; gap: 3px; }.guidance small { color: var(--text-secondary); }
    .detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 292px; align-items: start; gap: 18px; }.participants-panel { overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius-md); background: #fff; box-shadow: var(--shadow-sm); }.panel-tabs { display: flex; align-items: center; gap: 10px; min-height: 58px; padding: 0 18px; border-bottom: 1px solid var(--border); }.panel-tabs strong { align-self: stretch; display: flex; align-items: center; color: var(--mv-teal-dark); border-bottom: 3px solid var(--mv-teal); }.panel-tabs span { display: grid; place-items: center; min-width: 24px; height: 24px; border-radius: 12px; color: var(--mv-teal-dark); background: #e6f4f1; font-size: .72rem; font-weight: 800; }
    .participant-list { display: grid; }.participant-row { border: 0; border-bottom: 1px solid var(--border); border-radius: 0; box-shadow: none; }.participant-row:last-child { border-bottom: 0; }.participant-row:hover { transform: none; background: #fbfdfd; box-shadow: none !important; }.participant-row mat-card-content { display: grid; grid-template-columns: 245px 1fr; align-items: start; gap: 18px; padding: 20px; }.person-column { display: flex; align-items: center; gap: 13px; }.person-column > span:last-child { display: grid; gap: 4px; }.person-column small { color: var(--text-secondary); font-size: .76rem; }.avatar { display: grid; place-items: center; flex: 0 0 48px; width: 48px; height: 48px; border-radius: 50%; color: var(--mv-teal-dark); background: #e4f3ef; font-weight: 800; }
    .items-column { display: grid; gap: 7px; }.items-column > small { color: var(--text-secondary); font-size: .66rem; font-weight: 800; letter-spacing: .07em; text-transform: uppercase; }.item-line { display: grid; grid-template-columns: minmax(140px, 1fr) auto auto; align-items: center; gap: 7px; min-height: 39px; padding: 4px 8px; border: 1px solid var(--border); border-radius: 9px; background: #fbfcfc; }.item-name { display: flex; align-items: center; gap: 8px; font-size: .82rem; }.item-name mat-icon { width: 17px; height: 17px; color: var(--caramel); font-size: 17px; }
    .item-emoji { width: 20px; text-align: center; font-size: 1rem; }.status-pendente { --mdc-chip-elevated-container-color: #fff0df; color: #b96917; }.status-trouxe { --mdc-chip-elevated-container-color: #e0f2e9; color: #147156; }.status-nao_trouxe { --mdc-chip-elevated-container-color: #fde8e6; color: #b23d38; }.item-actions, .status-actions { display: flex; }.item-actions button, .status-actions button { width: 33px; height: 33px; }.status-actions button { color: var(--mv-teal); border: 1px solid #91d2cb; border-radius: 9px; }.status-actions .not-brought { color: var(--danger); border-color: #edaaa6; }.status-actions button.selected { background: #dff2ee; }.status-actions .not-brought.selected { background: #fbe5e3; }
    .participant-row mat-card-actions { justify-content: flex-end; gap: 6px; padding: 4px 12px 12px; }.danger-action { color: var(--danger); }.no-items { color: var(--text-secondary); font-size: .8rem; }
    .detail-aside { display: grid; gap: 16px; }.resume-card { border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }.resume-card mat-card-content { padding: 21px; }.resume-card h2 { display: flex; align-items: center; gap: 9px; margin: 0 0 26px; font-size: 1rem; }.resume-card h2 mat-icon { color: var(--mv-blue); }.participant-total { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid var(--border); }.participant-total > span { display: grid; gap: 5px; }.participant-total small { color: var(--text-secondary); }.participant-total strong { color: var(--mv-teal-dark); font-size: 2rem; }.participant-total > span:last-child { place-items: center; width: 54px; height: 54px; border-radius: 50%; color: var(--mv-teal); background: #e4f3ef; }
    .status-resume { display: grid; gap: 13px; padding-top: 19px; }.status-resume h3 { margin: 0 0 3px; font-size: .82rem; }.status-resume > span { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 8px; font-size: .75rem; font-weight: 700; text-transform: uppercase; }.status-resume i { display: grid; place-items: center; width: 19px; height: 19px; border-radius: 50%; }.status-resume i mat-icon { width: 12px; height: 12px; font-size: 12px; }.resume-success { color: #147156; }.resume-success i { background: #cce9dc; }.resume-warning { color: #bd6a10; }.resume-warning i { background: #fff0d9; }.resume-danger { color: #b23d38; }.resume-danger i { background: #f7d8d5; }.status-resume strong { font-size: 1rem; }
    .tip-card { display: flex; gap: 12px; padding: 18px; border: 1px solid #d8ebe6; border-radius: var(--radius-md); color: var(--mv-teal-dark); background: linear-gradient(130deg, #eff9f6, #e7f3ec); }.tip-card > mat-icon { flex: 0 0 24px; }.tip-card span { display: grid; gap: 7px; }.tip-card small { color: var(--text-secondary); line-height: 1.55; }
    @media (max-width: 1100px) { .detail-layout { grid-template-columns: 1fr; }.detail-aside { grid-template-columns: 1fr 1fr; }.participant-row mat-card-content { grid-template-columns: 210px 1fr; } }
    @media (max-width: 760px) { .detail-top > span, .detail-top > mat-icon, .detail-top > strong { display: none; }.cafe-summary { grid-template-columns: auto 1fr auto; gap: 14px; }.summary-divider { display: none; }.meeting-copy { grid-column: 1 / -1; }.phase-copy { grid-column: 2; }.participant-row mat-card-content { grid-template-columns: 1fr; }.detail-aside { grid-template-columns: 1fr; } }
    @media (max-width: 520px) { .cafe-summary { grid-template-columns: 1fr; }.date-symbol { width: 54px; height: 54px; }.phase-copy { grid-column: 1; }.item-line { grid-template-columns: 1fr auto; }.item-actions, .status-actions { grid-column: 1 / -1; justify-content: flex-end; }.guidance { align-items: stretch; flex-direction: column; } }
  `],
})
export class CafeDetailComponent implements OnInit {
  private readonly cafeService = inject(CafeService);
  private readonly collaboratorService = inject(ColaboradorService);
  private readonly participationService = inject(ParticipacaoService);
  private readonly itemService = inject(ItemService);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly cafe = signal<Cafe | null>(null);
  readonly colaboradores = signal<Colaborador[]>([]);
  readonly participacoes = signal<Participacao[]>([]);
  readonly loading = signal(true);
  readonly updatingStatus = signal<number | null>(null);
  readonly date = formatDate;
  readonly longDate = formatDateLong;
  readonly cpf = formatCpf;
  readonly isFuture = computed(() => !!this.cafe() && this.cafe()!.data > todayIso());
  readonly isToday = computed(() => this.cafe()?.data === todayIso());
  readonly phaseKey = computed(() => !this.cafe() ? '' : this.cafe()!.data > todayIso() ? 'future' : this.cafe()!.data === todayIso() ? 'today' : 'past');
  readonly phaseLabel = computed(() => !this.cafe() ? '' : this.cafe()!.data > todayIso() ? 'PRÓXIMO' : this.cafe()!.data === todayIso() ? 'HOJE' : 'FINALIZADO');
  readonly availableCollaborators = computed(() => {
    const used = new Set(this.participacoes().map((value) => value.colaboradorId));
    return this.colaboradores().filter((value) => !used.has(value.id));
  });

  ngOnInit(): void { this.load(); }
  collaborator(id: number): Colaborador | undefined { return this.colaboradores().find((value) => value.id === id); }
  initials(name?: string): string { return (name ?? '?').trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase(); }
  statusLabel(status: ItemStatus): string { return ({ PENDENTE: 'Pendente', TROUXE: 'Trouxe', NAO_TROUXE: 'Não trouxe' })[status]; }
  statusCount(status: ItemStatus): number { return this.participacoes().flatMap((value) => value.itens).filter((item) => item.status === status).length; }
  weekday(value: string): string { return new Intl.DateTimeFormat('pt-BR', { weekday: 'long' }).format(new Date(`${value}T12:00:00`)); }
  itemIcon(name: string): string {
    const normalized = name.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const icons: [string[], string][] = [
      [['cafe'], '☕'], [['suco'], '🧃'], [['agua'], '💧'], [['leite'], '🥛'], [['refrigerante'], '🥤'],
      [['pao'], '🥖'], [['bolo'], '🍰'], [['fruta'], '🍎'], [['queijo'], '🧀'], [['salgado'], '🥐'],
      [['copo'], '🥤'], [['prato'], '🍽️'], [['guardanapo'], '🧻'], [['talher', 'garfo', 'faca', 'colher'], '🍴'],
    ];
    return icons.find(([keywords]) => keywords.some((keyword) => normalized.includes(keyword)))?.[1] ?? '📦';
  }

  changeStatus(item: ItemCafe, status: Exclude<ItemStatus, 'PENDENTE'>): void {
    if (!this.isToday() || this.updatingStatus() !== null || item.status === status) return;
    this.updatingStatus.set(item.id);
    this.itemService.updateStatus(item.id, status).subscribe({
      next: (updated) => {
        this.participacoes.update((rows) => rows.map((participation) => ({ ...participation, itens: participation.itens.map((current) => current.id === updated.id ? updated : current) })));
        this.updatingStatus.set(null);
        this.success('Status atualizado com sucesso.');
      },
      error: (error) => { this.updatingStatus.set(null); this.showError(error); },
    });
  }

  addParticipant(): void {
    this.dialog.open(ParticipacaoDialogComponent, { width: '760px', maxWidth: '96vw', data: {
      cafeId: this.id,
      colaboradores: this.availableCollaborators(),
      itensReservados: this.participacoes().flatMap((participation) => participation.itens.map((item) => item.nome)),
    } }).afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;
      this.load();
      this.success('Participação confirmada com sucesso.');
    });
  }
  removeParticipant(participation: Participacao): void {
    const name = this.collaborator(participation.colaboradorId)?.nome ?? 'este participante';
    this.confirm('Remover participante?', `${name} e todos os itens associados serão removidos.`).subscribe((confirmed) => {
      if (!confirmed) return;
      this.participationService.delete(this.id, participation.id).subscribe({ next: () => { this.success('Participante removido.'); this.load(); }, error: (error) => this.showError(error) });
    });
  }
  addItem(participation: Participacao): void {
    this.dialog.open(ItemDialogComponent, { data: {} }).afterClosed().subscribe((name: string | undefined) => {
      if (!name) return;
      this.itemService.create(participation.id, name).subscribe({ next: () => { this.success('Item adicionado com sucesso.'); this.load(); }, error: (error) => this.showError(error) });
    });
  }
  editItem(item: ItemCafe): void {
    this.dialog.open(ItemDialogComponent, { data: { nome: item.nome } }).afterClosed().subscribe((name: string | undefined) => {
      if (!name) return;
      this.itemService.update(item.id, name).subscribe({ next: () => { this.success('Item atualizado.'); this.load(); }, error: (error) => this.showError(error) });
    });
  }
  deleteItem(item: ItemCafe): void {
    this.confirm('Excluir item?', `“${item.nome}” será removido deste café.`).subscribe((confirmed) => {
      if (!confirmed) return;
      this.itemService.delete(item.id).subscribe({ next: () => { this.success('Item excluído.'); this.load(); }, error: (error) => this.showError(error) });
    });
  }
  private load(): void {
    this.loading.set(true);
    forkJoin({ cafe: this.cafeService.get(this.id), colaboradores: this.collaboratorService.list(), participacoes: this.participationService.list(this.id) }).subscribe({
      next: ({ cafe, colaboradores, participacoes }) => { this.cafe.set(cafe); this.colaboradores.set(colaboradores); this.participacoes.set(participacoes); this.loading.set(false); },
      error: (error) => { this.loading.set(false); this.showError(error); },
    });
  }
  private confirm(title: string, message: string) { return this.dialog.open(ConfirmDialogComponent, { data: { title, message } }).afterClosed(); }
  private success(message: string): void { this.snackBar.open(message, 'Fechar', { duration: 3000 }); }
  private showError(error: unknown): void { this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 }); }
}
