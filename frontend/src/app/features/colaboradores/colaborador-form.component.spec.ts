import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { ColaboradorService } from '../../core/colaborador.service';
import { ColaboradorFormComponent } from './colaborador-form.component';

describe('ColaboradorFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ColaboradorFormComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } },
        { provide: ColaboradorService, useValue: { create: () => of({}) } },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('requires name and CPF before saving', () => {
    const component = TestBed.createComponent(ColaboradorFormComponent).componentInstance;
    component.save();
    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.nome.hasError('required')).toBe(true);
    expect(component.form.controls.cpf.hasError('required')).toBe(true);
  });

  it('validates CPF on the client', () => {
    const component = TestBed.createComponent(ColaboradorFormComponent).componentInstance;
    component.form.setValue({ nome: 'Maria', cpf: '123.456.789-01' });
    expect(component.form.controls.cpf.hasError('cpf')).toBe(true);
  });
});
