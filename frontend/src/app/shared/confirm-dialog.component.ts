import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle } from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

export interface ConfirmDialogData { title: string; message: string; confirmLabel?: string; }

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButton, MatIcon],
  template: `
    <h2 mat-dialog-title><span class="warning-icon"><mat-icon>delete_outline</mat-icon></span>{{ data.title }}</h2>
    <mat-dialog-content><p>{{ data.message }}</p><small>Esta ação não pode ser desfeita.</small></mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">{{ data.confirmLabel ?? 'Excluir' }}</button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 { display: flex; align-items: center; gap: 12px; padding-top: 22px; }
    .warning-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 14px; color: var(--danger); background: var(--danger-soft); }
    p { margin: 0 0 8px; line-height: 1.5; }
    small { color: var(--text-secondary); }
  `],
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
