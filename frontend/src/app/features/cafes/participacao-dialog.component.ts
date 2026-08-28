import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { apiErrorMessage } from '../../core/api-error';
import { formatCpf } from '../../core/cpf-utils';
import { Colaborador, ParticipacaoRequest } from '../../core/models';
import { ParticipacaoService } from '../../core/participacao.service';

export interface ParticipacaoDialogData {
  cafeId: number;
  colaboradores: Colaborador[];
  itensReservados: string[];
}

interface CatalogItem { key: string; nome: string; icon: string; }
interface CatalogCategory { label: string; icon: string; items: CatalogItem[]; }

export function normalizeItemName(value: string): string {
  return value.trim().replace(/\s+/g, ' ').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

@Component({
  selector: 'app-participacao-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatButton, MatIconButton, MatError, MatFormField, MatLabel, MatIcon, MatInput, MatOption, MatSelect, MatProgressSpinner],
  template: `
    <h2 mat-dialog-title><span class="dialog-icon"><mat-icon>person_add</mat-icon></span><span>Adicionar participante ao café<small>Escolha o colaborador e todos os itens que ele levará.</small></span></h2>
    @if (errorMessage()) { <div class="dialog-error" role="alert"><mat-icon>error</mat-icon><span>{{ errorMessage() }}</span></div> }
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Colaborador</mat-label>
          <mat-select formControlName="colaboradorId" placeholder="Selecione um colaborador" data-cy="colaborador-participacao">
            @for (person of data.colaboradores; track person.id) {
              <mat-option [value]="person.id">{{ person.nome }} — {{ cpf(person.cpf) }}</mat-option>
            }
          </mat-select>
          @if (form.controls.colaboradorId.hasError('required')) { <mat-error>Selecione um colaborador.</mat-error> }
        </mat-form-field>

        @if (selectedCollaborator(); as person) {
          <div class="selected-person" aria-label="Colaborador selecionado">
            <span class="selected-avatar"><mat-icon>person</mat-icon></span>
            <span><small>Colaborador selecionado</small><strong>{{ person.nome }}</strong><em>CPF {{ cpf(person.cpf) }}</em></span>
            <mat-icon class="selected-check">check_circle</mat-icon>
          </div>
        }

        <section class="selection-summary" aria-live="polite">
          <span><strong>Escolha os itens</strong><small>Selecione opções diferentes para este participante.</small></span>
          <strong class="counter" [class.complete]="hasItems()" data-cy="contador-itens">{{ itens.length }} {{ itens.length === 1 ? 'item escolhido' : 'itens escolhidos' }}</strong>
        </section>

        <div class="catalog">
          @for (category of catalog; track category.label) {
            <section class="catalog-category">
              <h3><mat-icon>{{ category.icon }}</mat-icon>{{ category.label }}</h3>
              <div class="catalog-grid">
                @for (item of category.items; track item.key) {
                  <button type="button" class="catalog-item" [class.selected]="isSelected(item.nome)" [class.unavailable]="isReserved(item.nome)" [disabled]="isDisabled(item.nome)" (click)="toggleItem(item.nome)" [attr.data-cy]="'item-' + item.key">
                    <span class="item-emoji" aria-hidden="true">{{ item.icon }}</span>
                    <span><strong>{{ item.nome }}</strong>@if (isReserved(item.nome)) { <small>Já escolhido neste café</small> } @else if (isSelected(item.nome)) { <small>Selecionado</small> }</span>
                    @if (isSelected(item.nome)) { <mat-icon>check_circle</mat-icon> }
                  </button>
                }
              </div>
            </section>
          }

          <section class="catalog-category other-category">
            <h3><mat-icon>more_horiz</mat-icon>Outros</h3>
            <button type="button" class="catalog-item other-button" [class.selected]="showOther()" (click)="toggleOther()" data-cy="item-outro">
              <span class="item-emoji" aria-hidden="true">✍️</span><span><strong>Outro</strong><small>Informe um item diferente</small></span>
            </button>
            @if (showOther()) {
              <div class="other-entry">
                <mat-form-field appearance="outline">
                  <mat-label>Digite o item</mat-label>
                  <input matInput [formControl]="customItem" maxlength="120" data-cy="outro-item-input" (keydown.enter)="$event.preventDefault(); addCustomItem()" />
                  @if (customItem.hasError('required')) { <mat-error>Informe o item.</mat-error> }
                  @if (customItem.hasError('maxlength')) { <mat-error>Use no máximo 120 caracteres.</mat-error> }
                </mat-form-field>
                <button mat-flat-button type="button" (click)="addCustomItem()" [disabled]="customItem.invalid" data-cy="adicionar-outro">Adicionar</button>
              </div>
            }
          </section>
        </div>

        <section class="chosen-items">
          <header><span><strong>Seus itens</strong><small>{{ itens.length }} {{ itens.length === 1 ? 'item selecionado' : 'itens selecionados' }}</small></span>@if (hasItems()) { <mat-icon>task_alt</mat-icon> }</header>
          @if (!itens.length) {
            <p>Nenhum item selecionado ainda.</p>
          } @else {
            <div class="chosen-list">
              @for (control of itens.controls; track control; let index = $index) {
                <span class="chosen-chip"><span>{{ itemIcon(control.value) }}</span>{{ control.value }}<button mat-icon-button type="button" (click)="removeItem(index)" [attr.aria-label]="'Remover ' + control.value"><mat-icon>close</mat-icon></button></span>
              }
            </div>
          }
        </section>

        @if (!hasItems()) { <p class="quantity-warning"><mat-icon>info</mat-icon>Selecione pelo menos um item para confirmar a participação.</p> }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()" [disabled]="saving()">Cancelar</button>
      <button mat-flat-button (click)="submit()" [disabled]="saving() || form.controls.colaboradorId.invalid || !hasItems()" data-cy="confirmar-participacao">
        @if (saving()) { <mat-spinner diameter="20" /> } @else { <span class="button-label"><mat-icon>check</mat-icon> Confirmar participação</span> }
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 { display: flex; align-items: center; gap: 13px; padding-top: 22px; }
    h2 > span:last-child { display: grid; gap: 4px; }
    h2 small { color: var(--text-secondary); font-size: .78rem; font-weight: 400; }
    .dialog-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 14px; color: var(--coffee-medium); background: var(--cream-deep); }
    mat-dialog-content { overflow-x: hidden; }
    .dialog-form { width: min(680px, 82vw); max-width: 100%; display: grid; gap: 15px; padding-top: 12px; }
    .selected-person { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 12px; margin-top: -4px; padding: 12px 14px; border: 1px solid #bcded9; border-radius: 13px; background: #f1faf8; }
    .selected-person > span:nth-child(2) { display: grid; gap: 2px; }.selected-person small { color: var(--text-secondary); font-size: .68rem; font-style: normal; text-transform: uppercase; }.selected-person strong { color: var(--mv-blue); }.selected-person em { color: var(--text-secondary); font-size: .76rem; font-style: normal; }
    .selected-avatar { display: grid; place-items: center; width: 39px; height: 39px; border-radius: 50%; color: var(--mv-teal); background: #dff2ee; }.selected-check { color: var(--mv-teal); }
    .selection-summary { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 13px 15px; border-radius: 13px; background: #f5f9f8; }
    .selection-summary > span { display: grid; gap: 3px; }.selection-summary small { color: var(--text-secondary); font-size: .75rem; }.counter { flex: 0 0 auto; padding: 7px 11px; border-radius: 18px; color: #a25d10; background: #fff0db; font-size: .76rem; }.counter.complete { color: #137159; background: #dff2eb; }
    .catalog { display: grid; gap: 15px; }.catalog-category { display: grid; gap: 8px; }.catalog-category h3 { display: flex; align-items: center; gap: 7px; margin: 0; color: var(--mv-blue); font-size: .82rem; }.catalog-category h3 mat-icon { width: 19px; height: 19px; color: var(--mv-teal); font-size: 19px; }
    .catalog-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }.catalog-item { position: relative; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 9px; min-height: 58px; padding: 8px 10px; text-align: left; color: var(--text-primary); border: 1px solid var(--border); border-radius: 12px; background: #fff; cursor: pointer; transition: 160ms ease; }.catalog-item:hover:not(:disabled) { border-color: #78beb5; transform: translateY(-1px); box-shadow: 0 5px 12px rgba(2, 76, 72, .08); }.catalog-item.selected { color: var(--mv-teal-dark); border-color: var(--mv-teal); background: #eaf7f4; }.catalog-item.unavailable { color: #8c969c; border-style: dashed; background: #f4f5f5; }.catalog-item:disabled { cursor: not-allowed; opacity: .62; }.catalog-item span:nth-child(2) { min-width: 0; display: grid; gap: 2px; }.catalog-item strong { overflow: hidden; font-size: .8rem; text-overflow: ellipsis; white-space: nowrap; }.catalog-item small { color: var(--text-secondary); font-size: .62rem; line-height: 1.2; }.catalog-item > mat-icon { width: 18px; height: 18px; color: var(--mv-teal); font-size: 18px; }.item-emoji { font-size: 1.25rem; }
    .other-category { padding-top: 2px; }.other-button { width: min(230px, 100%); }.other-entry { display: grid; grid-template-columns: 1fr auto; align-items: start; gap: 9px; }.other-entry mat-form-field { margin-bottom: -18px; }.other-entry > button { margin-top: 4px; }
    .chosen-items { overflow: hidden; border: 1px solid #c5e2dd; border-radius: 13px; background: #f6fbfa; }.chosen-items header { display: flex; justify-content: space-between; align-items: center; padding: 11px 13px; border-bottom: 1px solid #d7ebe7; }.chosen-items header span { display: grid; gap: 2px; }.chosen-items header small { color: var(--text-secondary); font-size: .7rem; }.chosen-items header > mat-icon { color: var(--mv-teal); }.chosen-items > p { margin: 0; padding: 14px; color: var(--text-secondary); font-size: .78rem; }.chosen-list { display: flex; flex-wrap: wrap; gap: 7px; padding: 11px; }.chosen-chip { display: flex; align-items: center; gap: 6px; min-height: 34px; padding: 2px 3px 2px 10px; border: 1px solid #a8d5cd; border-radius: 18px; color: var(--mv-teal-dark); background: #fff; font-size: .78rem; font-weight: 700; }.chosen-chip button { width: 28px; height: 28px; }.chosen-chip button mat-icon { width: 16px; height: 16px; font-size: 16px; }
    .quantity-warning, .dialog-error { display: flex; align-items: center; gap: 9px; margin: 0; padding: 11px 13px; border-radius: 12px; font-size: .8rem; }.quantity-warning { color: #93601f; border: 1px solid #efd19e; background: #fff7e8; }.dialog-error { margin: 0 24px 8px; color: var(--danger); border: 1px solid #edaaa6; background: var(--danger-soft); }.quantity-warning mat-icon, .dialog-error mat-icon { flex: 0 0 20px; width: 20px; height: 20px; font-size: 20px; }
    mat-dialog-actions button[mat-flat-button] { min-width: 205px; }
    @media (max-width: 700px) { .dialog-form { width: 100%; }.catalog-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.selection-summary { align-items: flex-start; flex-direction: column; }.other-entry { grid-template-columns: 1fr; } }
    @media (max-width: 430px) { .catalog-grid { grid-template-columns: 1fr; } }
  `],
})
export class ParticipacaoDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly participationService = inject(ParticipacaoService);
  readonly dialogRef = inject(MatDialogRef<ParticipacaoDialogComponent>);
  readonly data = inject<ParticipacaoDialogData>(MAT_DIALOG_DATA);
  readonly cpf = formatCpf;
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly showOther = signal(false);
  readonly customItem = this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]);
  readonly form = this.fb.group({
    colaboradorId: this.fb.control<number | null>(null, Validators.required),
    itens: this.fb.nonNullable.array<string>([]),
  });
  readonly catalog: CatalogCategory[] = [
    { label: 'Bebidas', icon: 'local_cafe', items: [
      { key: 'cafe', nome: 'Café', icon: '☕' }, { key: 'suco', nome: 'Suco', icon: '🧃' },
      { key: 'agua', nome: 'Água', icon: '💧' }, { key: 'leite', nome: 'Leite', icon: '🥛' },
      { key: 'refrigerante', nome: 'Refrigerante', icon: '🥤' },
    ] },
    { label: 'Comidas', icon: 'bakery_dining', items: [
      { key: 'pao', nome: 'Pão', icon: '🥖' }, { key: 'bolo', nome: 'Bolo', icon: '🍰' },
      { key: 'frutas', nome: 'Frutas', icon: '🍎' }, { key: 'queijo', nome: 'Queijo', icon: '🧀' },
      { key: 'salgados', nome: 'Salgados', icon: '🥐' },
    ] },
    { label: 'Talheres e utensílios', icon: 'flatware', items: [
      { key: 'copos', nome: 'Copos', icon: '🥤' }, { key: 'pratos', nome: 'Pratos', icon: '🍽️' },
      { key: 'guardanapos', nome: 'Guardanapos', icon: '🧻' }, { key: 'talheres', nome: 'Talheres', icon: '🍴' },
    ] },
  ];

  get itens(): FormArray { return this.form.controls.itens; }
  selectedCollaborator(): Colaborador | undefined { return this.data.colaboradores.find((person) => person.id === this.form.controls.colaboradorId.value); }
  hasItems(): boolean { return this.itens.length > 0; }
  isSelected(name: string): boolean { return this.itemIndex(name) >= 0; }
  isReserved(name: string): boolean { return this.data.itensReservados.some((item) => normalizeItemName(item) === normalizeItemName(name)); }
  isDisabled(name: string): boolean { return this.isReserved(name); }

  toggleItem(name: string): void {
    const index = this.itemIndex(name);
    if (index >= 0) { this.removeItem(index); return; }
    if (!this.isDisabled(name)) this.addItem(name);
  }

  toggleOther(): void {
    this.showOther.update((current) => !current);
    this.customItem.reset('');
    this.errorMessage.set('');
  }

  addCustomItem(): void {
    this.customItem.markAsTouched();
    const name = this.customItem.value.trim().replace(/\s+/g, ' ');
    if (this.customItem.invalid || !name) return;
    if (this.isSelected(name)) { this.errorMessage.set('Não repita o mesmo item neste pedido.'); return; }
    if (this.isReserved(name)) { this.errorMessage.set(`A opção '${name}' já foi cadastrada para este café.`); return; }
    this.addItem(name);
    this.customItem.reset('');
    this.showOther.set(false);
  }

  removeItem(index: number): void { this.itens.removeAt(index); this.errorMessage.set(''); }
  itemIcon(name: string): string { return this.catalog.flatMap((category) => category.items).find((item) => normalizeItemName(item.nome) === normalizeItemName(name))?.icon ?? '📦'; }

  submit(): void {
    if (this.form.controls.colaboradorId.invalid || !this.hasItems() || this.saving()) { this.form.markAllAsTouched(); return; }
    const request = { colaboradorId: this.form.controls.colaboradorId.value!, itens: this.itens.getRawValue().map((item) => item.trim()) } satisfies ParticipacaoRequest;
    this.saving.set(true);
    this.errorMessage.set('');
    this.participationService.create(this.data.cafeId, request).subscribe({
      next: () => this.dialogRef.close(true),
      error: (error) => { this.saving.set(false); this.errorMessage.set(apiErrorMessage(error)); },
    });
  }

  private addItem(name: string): void { this.itens.push(this.fb.nonNullable.control(name)); this.errorMessage.set(''); }
  private itemIndex(name: string): number { return this.itens.controls.findIndex((control) => normalizeItemName(control.value) === normalizeItemName(name)); }
}
