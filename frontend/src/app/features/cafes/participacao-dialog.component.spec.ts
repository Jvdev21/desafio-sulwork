import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ParticipacaoService } from '../../core/participacao.service';
import { ParticipacaoDialogComponent, ParticipacaoDialogData } from './participacao-dialog.component';

describe('ParticipacaoDialogComponent', () => {
  const close = vi.fn();
  const create = vi.fn();
  const dialogData: ParticipacaoDialogData = {
    cafeId: 7,
    colaboradores: [{ id: 1, nome: 'Maria', cpf: '52998224725', createdAt: '', updatedAt: '' }],
    itensReservados: ['Queijo'],
  };

  beforeEach(async () => {
    close.mockClear();
    create.mockReset();
    create.mockReturnValue(of({ id: 10, cafeId: 7, colaboradorId: 1, createdAt: '', itens: [] }));
    dialogData.itensReservados = ['Queijo'];
    await TestBed.configureTestingModule({
      imports: [ParticipacaoDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: { close } },
        { provide: ParticipacaoService, useValue: { create } },
      ],
    }).compileComponents();
  });

  it('exige pelo menos um item para confirmar', () => {
    const component = createComponent();
    component.form.controls.colaboradorId.setValue(1);
    expect(component.hasItems()).toBe(false);
    component.submit();
    expect(create).not.toHaveBeenCalled();
  });

  it('mantém segundo e terceiro itens selecionados simultaneamente', () => {
    const component = createComponent();
    component.toggleItem('Café');
    component.toggleItem('Pão');
    component.toggleItem('Bolo');
    expect(component.itens.getRawValue()).toEqual(['Café', 'Pão', 'Bolo']);
    expect(component.isSelected('cafe')).toBe(true);
    expect(component.isSelected('pão')).toBe(true);
    expect(component.isSelected('BOLO')).toBe(true);
  });

  it('remove somente o item selecionado novamente', () => {
    const component = createComponent();
    component.toggleItem('Café');
    component.toggleItem('Pão');
    component.toggleItem('CAFÉ');
    expect(component.itens.getRawValue()).toEqual(['Pão']);
  });

  it('não limita a quantidade de itens diferentes selecionados', () => {
    const component = createComponent();
    ['Café', 'Pão', 'Bolo', 'Suco', 'Água', 'Leite', 'Frutas'].forEach((item) => component.toggleItem(item));
    expect(component.itens.getRawValue()).toEqual(['Café', 'Pão', 'Bolo', 'Suco', 'Água', 'Leite', 'Frutas']);
    expect(component.isDisabled('Refrigerante')).toBe(false);
  });

  it('bloqueia item escolhido por outro participante e mantém item diferente disponível', () => {
    const component = createComponent();
    expect(component.isReserved(' QUÊIJO ')).toBe(true);
    expect(component.isDisabled('Queijo')).toBe(true);
    expect(component.isDisabled('Bolo')).toBe(false);
    component.toggleItem('Queijo');
    component.toggleItem('Bolo');
    expect(component.itens.getRawValue()).toEqual(['Bolo']);
  });

  it('adiciona a opção Outro sem substituir escolhas anteriores', () => {
    const component = createComponent();
    component.toggleItem('Café');
    component.toggleOther();
    component.customItem.setValue('Pão francês especial');
    component.addCustomItem();
    expect(component.itens.getRawValue()).toEqual(['Café', 'Pão francês especial']);
  });

  it('não aceita Outro duplicado ou reservado pelo normalizador', () => {
    const component = createComponent();
    component.toggleOther();
    component.customItem.setValue('  QUÊIJO  ');
    component.addCustomItem();
    expect(component.itens.length).toBe(0);
    expect(component.errorMessage()).toContain('já foi cadastrada');
  });

  it('permite confirmar com um ou mais itens em uma única participação', () => {
    const component = createComponent();
    component.form.controls.colaboradorId.setValue(1);
    component.toggleItem('Café');
    component.toggleItem('Pão');
    component.submit();
    expect(create).toHaveBeenCalledTimes(1);
    expect(create).toHaveBeenCalledWith(7, { colaboradorId: 1, itens: ['Café', 'Pão'] });
    expect(close).toHaveBeenCalledWith(true);
  });

  it('não renderiza contador restritivo X de N', () => {
    const fixture = TestBed.createComponent(ParticipacaoDialogComponent);
    fixture.componentInstance.toggleItem('Café');
    fixture.componentInstance.toggleItem('Pão');
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('2 itens escolhidos');
    expect(text).not.toMatch(/\d+\s+de\s+\d+/);
  });

  function createComponent(): ParticipacaoDialogComponent {
    return TestBed.createComponent(ParticipacaoDialogComponent).componentInstance;
  }
});
