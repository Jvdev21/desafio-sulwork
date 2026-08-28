export function todayIso(): string {
  return toIsoDate(new Date());
}

export function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function parseIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function formatDate(value: string): string {
  return new Intl.DateTimeFormat('pt-BR').format(parseIsoDate(value));
}

export function formatDateLong(value: string): string {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' }).format(parseIsoDate(value));
}

export function formatDay(value: string): string {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit' }).format(parseIsoDate(value));
}

export function formatShortMonth(value: string): string {
  return new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(parseIsoDate(value)).replace('.', '').toUpperCase();
}
