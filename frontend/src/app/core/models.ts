export interface Colaborador {
  id: number;
  nome: string;
  cpf: string;
  createdAt: string;
  updatedAt: string;
}

export interface ColaboradorRequest {
  nome: string;
  cpf: string;
}

export interface Cafe {
  id: number;
  data: string;
  createdAt: string;
}

export interface CafeRequest {
  data: string;
}

export type ItemStatus = 'PENDENTE' | 'TROUXE' | 'NAO_TROUXE';

export interface ItemCafe {
  id: number;
  participacaoId: number;
  cafeId: number;
  nome: string;
  status: ItemStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Participacao {
  id: number;
  cafeId: number;
  colaboradorId: number;
  createdAt: string;
  itens: ItemCafe[];
}

export interface ParticipacaoRequest {
  colaboradorId: number;
  itens: string[];
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  fields?: Record<string, string>;
}
