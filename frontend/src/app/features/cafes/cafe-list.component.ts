import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage } from '../../core/api-error';
import { CafeService } from '../../core/cafe.service';
import { formatDate, formatDateLong, todayIso } from '../../core/date-utils';
import { Cafe } from '../../core/models';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  selector: 'app-cafe-list',
  standalone: true,
  imports: [RouterLink, MatButton, MatIconButton, MatCard, MatCardContent, MatChipsModule, MatIcon, MatProgressSpinner],
  template: `
    <header class="page-header">
      <div><h1>Cafés da manhã</h1><p>Organize os próximos encontros da equipe.</p></div>
      <a mat-flat-button routerLink="/cafes/novo"><mat-icon>add</mat-icon> Novo café da manhã</a>
    </header>

    @if (loading()) {
      <div class="loading"><mat-spinner diameter="42" /></div>
    } @else {
      <section class="summary-grid" aria-label="Resumo dos cafés">
        @for (section of sections(); track section.title) {
          <mat-card appearance="outlined" class="summary-card interactive-card">
            <mat-card-content><span class="summary-icon" [class]="'summary-' + section.key"><mat-icon>{{ section.icon }}</mat-icon></span><span><small>{{ section.title }}</small><strong>{{ section.items.length }}</strong><em>{{ section.description }}</em></span></mat-card-content>
          </mat-card>
        }
      </section>

      @if (!cafes().length) {
        <div class="empty-state"><span class="empty-icon"><mat-icon>free_breakfast</mat-icon></span><h2>Nenhum café da manhã agendado</h2><p>Escolha uma data futura e comece a reunir a equipe.</p><a mat-flat-button routerLink="/cafes/novo"><mat-icon>add</mat-icon> Criar primeiro café</a></div>
      } @else {
        <section class="list-panel">
          <div class="panel-title"><span><mat-icon>free_breakfast</mat-icon><h2>Cafés da manhã</h2></span><small>{{ cafes().length }} {{ cafes().length === 1 ? 'registro' : 'registros' }}</small></div>
          <div class="coffee-list" role="table" aria-label="Lista de cafés da manhã">
            <div class="list-header" role="row"><span>Data</span><span>Encontro</span><span>Status</span><span>Ações</span></div>
            @for (cafe of orderedCafes(); track cafe.id) {
              <div class="coffee-row" role="row">
                <span class="date-cell"><span class="date-icon"><mat-icon>event_available</mat-icon></span><span><strong>{{ date(cafe.data) }}</strong><small>{{ longDate(cafe.data) }}</small></span></span>
                <span class="meeting-cell"><strong>Café da manhã</strong><small>Encontro da equipe</small></span>
                <span><mat-chip [class]="'phase-' + phaseKey(cafe)">{{ phaseLabel(cafe) }}</mat-chip></span>
                <span class="row-actions">
                  <a mat-stroked-button [routerLink]="['/cafes', cafe.id]">Ver detalhes</a>
                  @if (isFuture(cafe)) {
                    <a mat-icon-button [routerLink]="['/cafes', cafe.id, 'editar']" aria-label="Editar café"><mat-icon>edit</mat-icon></a>
                    <button mat-icon-button class="danger-action" (click)="confirmDelete(cafe)" aria-label="Excluir café"><mat-icon>delete</mat-icon></button>
                  }
                </span>
              </div>
            }
          </div>
          <footer>Mostrando {{ cafes().length }} {{ cafes().length === 1 ? 'café' : 'cafés' }}</footer>
        </section>
      }
    }
  `,
  styles: [`
    .summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-bottom: 22px; }
    .summary-card { border-radius: var(--radius-md); box-shadow: var(--shadow-sm); background: #fff; }
    .summary-card mat-card-content { display: flex; align-items: center; gap: 18px; min-height: 122px; padding: 20px 22px; }
    .summary-card mat-card-content > span:last-child { display: grid; gap: 3px; }
    .summary-icon { display: grid; place-items: center; flex: 0 0 68px; width: 68px; height: 68px; border-radius: 50%; color: var(--mv-teal); background: #e8f6f3; }
    .summary-icon mat-icon { width: 34px; height: 34px; font-size: 34px; }
    .summary-today { color: #b56a18; background: #fff3e3; }.summary-past { color: var(--mv-blue); background: #edf3f8; }
    .summary-card small { color: var(--text-primary); font-weight: 700; }.summary-card strong { color: var(--mv-teal-dark); font-size: 1.9rem; line-height: 1; }.summary-today + span strong { color: #c57520; }.summary-card em { color: var(--text-secondary); font-size: .78rem; font-style: normal; }
    .list-panel { overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius-md); background: #fff; box-shadow: var(--shadow-sm); animation: card-in 350ms ease both; }
    .panel-title { display: flex; align-items: center; justify-content: space-between; min-height: 68px; padding: 0 22px; border-bottom: 1px solid var(--border); }
    .panel-title > span { display: flex; align-items: center; gap: 10px; }.panel-title mat-icon { color: var(--mv-teal); }.panel-title h2 { margin: 0; font-size: 1.2rem; }.panel-title small { color: var(--text-secondary); }
    .coffee-list { min-width: 760px; }.list-header, .coffee-row { display: grid; grid-template-columns: 1.2fr 1.4fr .75fr 1fr; align-items: center; gap: 18px; padding: 0 22px; }
    .list-header { min-height: 42px; color: var(--text-secondary); background: #f8fafb; font-size: .7rem; font-weight: 800; letter-spacing: .07em; text-transform: uppercase; }
    .coffee-row { min-height: 82px; border-top: 1px solid var(--border); transition: background var(--transition); }.coffee-row:hover { background: #f9fcfc; }
    .date-cell { display: flex; align-items: center; gap: 12px; }.date-cell > span:last-child, .meeting-cell { display: grid; gap: 3px; }.date-cell small, .meeting-cell small { color: var(--text-secondary); font-size: .75rem; }
    .date-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 11px; color: var(--mv-teal); background: #e7f5f2; }
    .row-actions { display: flex; align-items: center; justify-content: flex-end; gap: 4px; }.row-actions > a[mat-stroked-button] { color: var(--mv-teal-dark); border-color: #a8d8d3; }.danger-action { color: var(--danger); }
    .phase-future { --mdc-chip-elevated-container-color: #e4f3ec; color: #16745d; }.phase-today { --mdc-chip-elevated-container-color: #fff0df; color: #b96917; }.phase-past { --mdc-chip-elevated-container-color: #eaf0f7; color: var(--mv-blue); }
    footer { padding: 15px 22px; border-top: 1px solid var(--border); color: var(--text-secondary); font-size: .78rem; }
    @media (max-width: 1050px) { .summary-grid { grid-template-columns: 1fr; }.list-panel { overflow-x: auto; } }
    @media (max-width: 650px) { .summary-card mat-card-content { min-height: 98px; }.summary-icon { flex-basis: 56px; width: 56px; height: 56px; }.coffee-list { min-width: 0; }.list-header { display: none; }.coffee-row { grid-template-columns: 1fr auto; gap: 12px; margin: 12px; padding: 16px; border: 1px solid var(--border); border-radius: 14px; }.meeting-cell { grid-column: 1; }.coffee-row > span:nth-child(3) { grid-column: 2; grid-row: 1; }.row-actions { grid-column: 1 / -1; justify-content: flex-start; padding-top: 8px; border-top: 1px solid var(--border); } }
  `],
})
export class CafeListComponent implements OnInit {
  private readonly service = inject(CafeService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly cafes = signal<Cafe[]>([]);
  readonly loading = signal(true);
  readonly date = formatDate;
  readonly longDate = formatDateLong;
  readonly sections = computed(() => {
    const today = todayIso();
    return [
      { title: 'Próximos', label: 'PRÓXIMO', key: 'future', icon: 'event_available', description: 'Cafés agendados', items: this.cafes().filter((c) => c.data > today) },
      { title: 'Hoje', label: 'HOJE', key: 'today', icon: 'free_breakfast', description: 'Programado para hoje', items: this.cafes().filter((c) => c.data === today) },
      { title: 'Finalizados', label: 'FINALIZADO', key: 'past', icon: 'check_circle', description: 'Cafés realizados', items: this.cafes().filter((c) => c.data < today) },
    ];
  });
  readonly orderedCafes = computed(() => [...this.cafes()].sort((a, b) => b.data.localeCompare(a.data)));

  ngOnInit(): void { this.load(); }
  isFuture(cafe: Cafe): boolean { return cafe.data > todayIso(); }
  phaseKey(cafe: Cafe): string { return cafe.data > todayIso() ? 'future' : cafe.data === todayIso() ? 'today' : 'past'; }
  phaseLabel(cafe: Cafe): string { return cafe.data > todayIso() ? 'PRÓXIMO' : cafe.data === todayIso() ? 'HOJE' : 'FINALIZADO'; }

  confirmDelete(cafe: Cafe): void {
    this.dialog.open(ConfirmDialogComponent, { data: { title: 'Excluir café?', message: `O café de ${formatDate(cafe.data)} e todas as participações serão removidos.` } })
      .afterClosed().subscribe((confirmed) => {
        if (!confirmed) return;
        this.service.delete(cafe.id).subscribe({
          next: () => { this.snackBar.open('Café excluído com sucesso.', 'Fechar', { duration: 3000 }); this.load(); },
          error: (error) => this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 }),
        });
      });
  }

  private load(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (cafes) => { this.cafes.set(cafes); this.loading.set(false); },
      error: (error) => { this.loading.set(false); this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 }); },
    });
  }
}
