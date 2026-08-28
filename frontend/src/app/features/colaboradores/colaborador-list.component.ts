import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { apiErrorMessage } from '../../core/api-error';
import { formatCpf } from '../../core/cpf-utils';
import { Colaborador } from '../../core/models';
import { ColaboradorService } from '../../core/colaborador.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  selector: 'app-colaborador-list',
  standalone: true,
  imports: [RouterLink, MatButton, MatIconButton, MatCard, MatIcon, MatProgressSpinner, MatTableModule],
  template: `
    <header class="page-header">
      <div><h1>Colaboradores</h1><p>Gerencie os membros da equipe que participam dos cafés da manhã.</p></div>
      <a mat-flat-button routerLink="/colaboradores/novo"><mat-icon>person_add</mat-icon> Cadastrar colaborador</a>
    </header>

    @if (loading()) {
      <div class="loading"><mat-spinner diameter="42" /></div>
    } @else {
      <mat-card appearance="outlined" class="people-panel">
        @if (colaboradores().length) {
          <div class="table-wrap">
            <table mat-table [dataSource]="colaboradores()">
              <ng-container matColumnDef="nome">
                <th mat-header-cell *matHeaderCellDef>Nome</th>
                <td mat-cell *matCellDef="let row"><div class="person-cell"><span class="avatar">{{ initials(row.nome) }}</span><span><strong>{{ row.nome }}</strong><small>Colaborador</small></span></div></td>
              </ng-container>
              <ng-container matColumnDef="cpf">
                <th mat-header-cell *matHeaderCellDef>CPF</th>
                <td mat-cell *matCellDef="let row">{{ cpf(row.cpf) }}</td>
              </ng-container>
              <ng-container matColumnDef="acoes">
                <th mat-header-cell *matHeaderCellDef class="action-cell">Ações</th>
                <td mat-cell *matCellDef="let row" class="action-cell">
                  <a mat-icon-button [routerLink]="['/colaboradores', row.id, 'editar']" aria-label="Editar"><mat-icon>edit</mat-icon></a>
                  <button mat-icon-button class="danger-action" (click)="confirmDelete(row)" aria-label="Excluir"><mat-icon>delete</mat-icon></button>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="columns"></tr>
              <tr mat-row *matRowDef="let row; columns: columns"></tr>
            </table>
          </div>
        } @else {
          <div class="empty-state"><span class="empty-icon"><mat-icon>group</mat-icon></span><h2>Ainda não há colaboradores cadastrados</h2><p>Cadastre o primeiro colaborador para começar a organizar os cafés da manhã.</p><a mat-flat-button routerLink="/colaboradores/novo"><mat-icon>person_add</mat-icon> Cadastrar colaborador</a></div>
        }
      </mat-card>
    }
  `,
  styles: [`
    .people-panel { overflow: hidden; border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); animation: card-in 380ms ease both; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; }
    th { color: var(--text-secondary); font-size: .73rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; background: var(--surface-soft); }
    td { height: 76px; border-color: var(--border); }
    tr.mat-mdc-row { transition: background var(--transition); }
    tr.mat-mdc-row:hover { background: #fcf8f2; }
    .person-cell { display: flex; align-items: center; gap: 13px; }
    .person-cell > span:last-child { display: grid; gap: 3px; }
    .person-cell small { color: var(--text-secondary); font-size: .76rem; }
    .avatar { display: grid; place-items: center; flex: 0 0 43px; width: 43px; height: 43px; border-radius: 14px; color: #fff; background: linear-gradient(145deg, var(--coffee-medium), var(--coffee-soft)); font-size: .78rem; font-weight: 800; box-shadow: 0 5px 12px rgb(101 70 56 / 18%); }
    .action-cell { width: 120px; text-align: right; white-space: nowrap; }
    .danger-action { color: var(--danger); }
    @media (max-width: 650px) {
      .people-panel { border: 0; background: transparent; box-shadow: none; overflow: visible; }
      table, tbody { display: block; background: transparent; }
      thead { display: none; }
      tr.mat-mdc-row { display: grid; grid-template-columns: 1fr auto; gap: 8px; height: auto; margin-bottom: 12px; padding: 16px; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--surface); box-shadow: var(--shadow-sm); }
      td.mat-mdc-cell { display: block; width: auto; height: auto; padding: 0; border: 0; }
      td.mat-column-cpf { grid-column: 1; padding-left: 56px; color: var(--text-secondary); }
      td.action-cell { grid-column: 2; grid-row: 1 / span 2; align-self: center; }
    }
  `],
})
export class ColaboradorListComponent implements OnInit {
  private readonly service = inject(ColaboradorService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly colaboradores = signal<Colaborador[]>([]);
  readonly loading = signal(true);
  readonly columns = ['nome', 'cpf', 'acoes'];
  readonly cpf = formatCpf;

  ngOnInit(): void { this.load(); }
  initials(name: string): string { return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase(); }

  confirmDelete(colaborador: Colaborador): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir colaborador?', message: `${colaborador.nome} será removido da lista de colaboradores.` },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.service.delete(colaborador.id).subscribe({
        next: () => { this.snackBar.open('Colaborador excluído com sucesso.', 'Fechar', { duration: 3000 }); this.load(); },
        error: (error) => this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 }),
      });
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (rows) => { this.colaboradores.set(rows); this.loading.set(false); },
      error: (error) => { this.loading.set(false); this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 }); },
    });
  }
}
