import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'app-item-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatButton, MatError, MatFormField, MatLabel, MatInput, MatIcon],
  template: `
    <h2 mat-dialog-title><span class="dialog-icon"><mat-icon>bakery_dining</mat-icon></span><span>{{ data.nome ? 'Editar item' : 'Adicionar item' }}<small>Informe o que o colaborador levará.</small></span></h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Nome do item</mat-label>
          <input matInput formControlName="nome" maxlength="120" placeholder="Ex.: Suco de laranja" />
          @if (form.controls.nome.hasError('required')) { <mat-error>Nome do item é obrigatório.</mat-error> }
          @if (form.controls.nome.hasError('maxlength')) { <mat-error>Máximo de 120 caracteres.</mat-error> }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Cancelar</button>
      <button mat-flat-button (click)="submit()"><mat-icon>check</mat-icon> Salvar item</button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 { display: flex; align-items: center; gap: 13px; padding-top: 22px; }
    h2 > span:last-child { display: grid; gap: 4px; }
    h2 small { color: var(--text-secondary); font-size: .78rem; font-weight: 400; }
    .dialog-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 14px; color: var(--coffee-medium); background: var(--cream-deep); }
    mat-dialog-content { overflow-x: hidden; }
    .dialog-form { width: min(420px, 74vw); max-width: 100%; padding-top: 12px; }
    mat-form-field { width: 100%; }
  `],
})
export class ItemDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly dialogRef = inject(MatDialogRef<ItemDialogComponent>);
  readonly data = inject<{ nome?: string }>(MAT_DIALOG_DATA);
  readonly form = this.fb.nonNullable.group({ nome: [this.data.nome ?? '', [Validators.required, Validators.maxLength(120)]] });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.getRawValue().nome.trim());
  }
}
