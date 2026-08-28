import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { formatDate, toIsoDate } from './date-utils';

describe('Brazilian date configuration', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
        provideNativeDateAdapter(),
      ],
    });
  });

  it('formats the Material date input as DD/MM/YYYY', () => {
    const adapter = TestBed.inject(DateAdapter<Date>);
    const formats = TestBed.inject(MAT_DATE_FORMATS);
    expect(adapter.format(new Date(2026, 7, 27), formats.display.dateInput)).toBe('27/08/2026');
  });

  it('shows ISO dates in Brazilian format while preserving the API format', () => {
    expect(formatDate('2026-08-27')).toBe('27/08/2026');
    expect(toIsoDate(new Date(2026, 7, 27))).toBe('2026-08-27');
  });
});
