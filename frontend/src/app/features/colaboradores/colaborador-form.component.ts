import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage, apiFieldErrors } from '../../core/api-error';
import { CpfMaskDirective } from '../../core/cpf-mask.directive';
import { cpfValidator, formatCpf } from '../../core/cpf-utils';
import { ColaboradorService } from '../../core/colaborador.service';

@Component({
  selector: 'app-colaborador-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CpfMaskDirective, MatButton, MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardTitle, MatError, MatFormField, MatLabel, MatInput, MatIcon, MatProgressSpinner],
  template: `
    <header class="page-header form-heading">
      <div><h1>{{ editing() ? 'Editar colaborador' : 'Novo colaborador' }}</h1><p>Informe os dados da pessoa que participará dos cafés da manhã.</p></div>
    </header>
    <mat-card class="form-card" appearance="outlined">
      <mat-card-header><mat-card-title><span class="form-title"><span class="form-icon"><mat-icon>person</mat-icon></span>Dados do colaborador</span></mat-card-title></mat-card-header>
      <mat-card-content>
        <form class="form-grid" [formGroup]="form" (ngSubmit)="save()" id="collaborator-form">
          <mat-form-field appearance="outline">
            <mat-label>Nome completo</mat-label>
            <input matInput formControlName="nome" maxlength="150" autocomplete="name" placeholder="Ex.: João da Silva" />
            @if (form.controls.nome.hasError('required')) { <mat-error>Nome é obrigatório.</mat-error> }
            @if (form.controls.nome.hasError('maxlength')) { <mat-error>Máximo de 150 caracteres.</mat-error> }
            @if (form.controls.nome.hasError('server')) { <mat-error>{{ fieldError('nome') }}</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>CPF</mat-label>
            <input matInput appCpfMask formControlName="cpf" maxlength="14" inputmode="numeric" autocomplete="off" placeholder="000.000.000-00" />
            @if (form.controls.cpf.hasError('required')) { <mat-error>CPF é obrigatório.</mat-error> }
            @if (form.controls.cpf.hasError('cpf')) { <mat-error>CPF inválido.</mat-error> }
            @if (form.controls.cpf.hasError('server')) { <mat-error>{{ fieldError('cpf') }}</mat-error> }
          </mat-form-field>
        </form>
      </mat-card-content>
      <mat-card-actions align="end">
        <a mat-button routerLink="/colaboradores">Cancelar</a>
        <button mat-flat-button type="submit" form="collaborator-form" [disabled]="saving()">
          @if (saving()) { <mat-spinner diameter="20" /> } @else { <span class="button-label"><mat-icon>check</mat-icon> Salvar colaborador</span> }
        </button>
      </mat-card-actions>
    </mat-card>
  `,
})
export class ColaboradorFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ColaboradorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private id: number | null = null;
  private serverFields: Record<string, string> = {};
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    cpf: ['', [Validators.required, cpfValidator]],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;
    this.id = id;
    this.editing.set(true);
    this.service.get(id).subscribe({
      next: (value) => this.form.setValue({ nome: value.nome, cpf: formatCpf(value.cpf) }),
      error: (error) => this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 }),
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const request = this.form.getRawValue();
    const operation = this.id ? this.service.update(this.id, request) : this.service.create(request);
    operation.subscribe({
      next: () => {
        this.snackBar.open(`Colaborador ${this.id ? 'atualizado' : 'cadastrado'} com sucesso.`, 'Fechar', { duration: 3000 });
        void this.router.navigate(['/colaboradores']);
      },
      error: (error) => {
        this.saving.set(false);
        this.serverFields = apiFieldErrors(error);
        Object.keys(this.serverFields).forEach((field) => this.form.get(field)?.setErrors({ server: true }));
        this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 });
      },
    });
  }

  fieldError(field: string): string { return this.serverFields[field] ?? 'Valor inválido.'; }
}
