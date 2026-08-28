import { FormControl } from '@angular/forms';
import { cpfValidator, formatCpf } from './cpf-utils';

describe('CPF utilities', () => {
  it('formats a normalized CPF', () => expect(formatCpf('52998224725')).toBe('529.982.247-25'));

  it('accepts a valid CPF and rejects an invalid one', () => {
    expect(cpfValidator(new FormControl('529.982.247-25'))).toBeNull();
    expect(cpfValidator(new FormControl('123.456.789-01'))).toEqual({ cpf: true });
  });
});
