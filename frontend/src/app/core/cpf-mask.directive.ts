import { Directive, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';
import { formatCpf } from './cpf-utils';

@Directive({
  selector: 'input[appCpfMask]',
  standalone: true,
})
export class CpfMaskDirective {
  private readonly control = inject(NgControl);

  @HostListener('input', ['$event'])
  onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatCpf(input.value);
    input.value = formatted;
    this.control.control?.setValue(formatted, { emitEvent: false });
  }
}
