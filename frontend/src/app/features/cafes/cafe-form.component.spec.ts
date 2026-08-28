import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { CafeService } from '../../core/cafe.service';
import { CafeFormComponent } from './cafe-form.component';

describe('CafeFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CafeFormComponent],
      providers: [
        provideNoopAnimations(),
        provideNativeDateAdapter(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } },
        { provide: CafeService, useValue: {} },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('mantém somente a data no formulário do café', () => {
    const component = TestBed.createComponent(CafeFormComponent).componentInstance;
    expect(Object.keys(component.form.controls)).toEqual(['data']);
  });

  it('exige a data do café', () => {
    const component = TestBed.createComponent(CafeFormComponent).componentInstance;
    expect(component.form.controls.data.hasError('required')).toBe(true);
    component.form.controls.data.setValue(new Date(2030, 0, 1));
    expect(component.form.controls.data.valid).toBe(true);
  });
});
