import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { CafeService } from '../../core/cafe.service';
import { todayIso } from '../../core/date-utils';
import { CafeListComponent } from './cafe-list.component';

describe('CafeListComponent', () => {
  it('loads cafes and separates future, today and finished records', async () => {
    const today = todayIso();
    const date = new Date();
    date.setDate(date.getDate() + 1);
    const tomorrow = date.toISOString().slice(0, 10);
    date.setDate(date.getDate() - 2);
    const yesterday = date.toISOString().slice(0, 10);

    await TestBed.configureTestingModule({
      imports: [CafeListComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: CafeService, useValue: { list: () => of([
          { id: 1, data: tomorrow, createdAt: '' },
          { id: 2, data: today, createdAt: '' },
          { id: 3, data: yesterday, createdAt: '' },
        ]) } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CafeListComponent);
    fixture.detectChanges();
    const sections = fixture.componentInstance.sections();
    expect(sections.map((section) => section.items.length)).toEqual([1, 1, 1]);
  });
});
