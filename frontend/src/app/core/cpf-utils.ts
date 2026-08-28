import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function formatCpf(value: string | null | undefined): string {
  const digits = (value ?? '').replace(/\D/g, '').slice(0, 11);
  return digits
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2');
}

export const cpfValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const cpf = String(control.value ?? '').replace(/\D/g, '');
  if (!cpf) return null;
  if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return { cpf: true };
  const digit = (length: number): number => {
    let sum = 0;
    for (let index = 0; index < length; index++) sum += Number(cpf[index]) * (length + 1 - index);
    const result = (sum * 10) % 11;
    return result === 10 ? 0 : result;
  };
  return digit(9) === Number(cpf[9]) && digit(10) === Number(cpf[10]) ? null : { cpf: true };
};
