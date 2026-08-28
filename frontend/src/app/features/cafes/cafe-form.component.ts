import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatError, MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage, apiFieldErrors } from '../../core/api-error';
import { CafeService } from '../../core/cafe.service';
import { parseIsoDate, toIsoDate } from '../../core/date-utils';

@Component({
  selector: 'app-cafe-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButton, MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardTitle, MatDatepicker, MatDatepickerInput, MatDatepickerToggle, MatError, MatFormField, MatLabel, MatSuffix, MatInput, MatIcon, MatProgressSpinner],
  template: `
    <header class="page-header form-heading"><div><h1>{{ editing() ? 'Editar café da manhã' : 'Novo café da manhã' }}</h1><p>Defina a data do encontro da equipe.</p></div></header>
    <mat-card class="form-card" appearance="outlined">
      <mat-card-header><mat-card-title><span class="form-title"><span class="form-icon"><mat-icon>calendar_month</mat-icon></span>Quando será o café?</span></mat-card-title></mat-card-header>
      <mat-card-content>
        <form class="form-grid" [formGroup]="form" (ngSubmit)="save()" id="cafe-form">
          <mat-form-field appearance="outline">
            <mat-label>Data</mat-label>
            <input matInput [matDatepicker]="picker" [min]="minDate" formControlName="data" readonly />
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-datepicker #picker />
            @if (form.controls.data.hasError('required')) { <mat-error>Data é obrigatória.</mat-error> }
            @if (form.controls.data.hasError('matDatepickerMin')) { <mat-error>Escolha uma data posterior a hoje.</mat-error> }
            @if (form.controls.data.hasError('server')) { <mat-error>{{ fieldError }}</mat-error> }
          </mat-form-field>
        </form>
      </mat-card-content>
      <mat-card-actions align="end">
        <a mat-button routerLink="/cafes">Cancelar</a>
        <button mat-flat-button type="submit" form="cafe-form" [disabled]="saving()">
          @if (saving()) { <mat-spinner diameter="20" /> } @else { <span class="button-label"><mat-icon>check</mat-icon> {{ editing() ? 'Salvar alterações' : 'Criar café' }}</span> }
        </button>
      </mat-card-actions>
    </mat-card>
  `,
})
export class CafeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CafeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private id: number | null = null;
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly minDate = (() => { const value = new Date(); value.setHours(0, 0, 0, 0); value.setDate(value.getDate() + 1); return value; })();
  readonly form = this.fb.group({
    data: this.fb.control<Date | null>(null, Validators.required),
  });
  fieldError = 'Data inválida.';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;
    this.id = id;
    this.editing.set(true);
    this.service.get(id).subscribe({
      next: (cafe) => this.form.setValue({
        data: parseIsoDate(cafe.data),
      }),
      error: (error) => this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 }),
    });
  }

  save(): void {
    if (this.form.invalid || !this.form.value.data) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const request = {
      data: toIsoDate(this.form.value.data),
    };
    const operation = this.id ? this.service.update(this.id, request) : this.service.create(request);
    operation.subscribe({
      next: (cafe) => {
        this.snackBar.open(`Café ${this.id ? 'atualizado' : 'cadastrado'} com sucesso.`, 'Fechar', { duration: 3000 });
        void this.router.navigate(['/cafes', cafe.id]);
      },
      error: (error) => {
        this.saving.set(false);
        const fields = apiFieldErrors(error);
        this.fieldError = fields['data'] ?? apiErrorMessage(error);
        if (fields['data']) this.form.controls.data.setErrors({ server: true });
        this.snackBar.open(apiErrorMessage(error), 'Fechar', { duration: 5000 });
      },
    });
  }
}
