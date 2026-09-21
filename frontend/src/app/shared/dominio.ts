import { ParticipanteResponse, PeladaResumoResponse, UsuarioRequest } from '../api';

/**
 * Rótulos dos enums para **selects e filtros**. Nas respostas da API não são
 * necessários: o backend já manda `posicaoDescricao`, `statusDescricao` e
 * `tipoCampoDescricao` junto do valor.
 */
export type Posicao = NonNullable<UsuarioRequest['posicao']>;
export type TipoCampo = NonNullable<PeladaResumoResponse['tipoCampo']>;
export type StatusPelada = NonNullable<PeladaResumoResponse['status']>;
export type StatusParticipacao = NonNullable<ParticipanteResponse['status']>;

export interface Opcao<T> {
  readonly valor: T;
  readonly rotulo: string;
}

export const POSICOES: readonly Opcao<Posicao>[] = [
  { valor: 'GOLEIRO', rotulo: 'Goleiro' },
  { valor: 'ALA', rotulo: 'Ala' },
  { valor: 'FIXO', rotulo: 'Fixo' },
  { valor: 'PIVO', rotulo: 'Pivô' },
  { valor: 'AMADOR', rotulo: 'Amador' },
];

export const TIPOS_DE_CAMPO: readonly Opcao<TipoCampo>[] = [
  { valor: 'FUTSAL', rotulo: 'Futsal' },
  { valor: 'SOCIETY', rotulo: 'Society' },
];

export const STATUS_DE_PELADA: readonly Opcao<StatusPelada>[] = [
  { valor: 'AGENDADA', rotulo: 'Agendada' },
  { valor: 'CONFIRMADA', rotulo: 'Confirmada' },
  { valor: 'EM_ANDAMENTO', rotulo: 'Em andamento' },
  { valor: 'FINALIZADA', rotulo: 'Finalizada' },
  { valor: 'CANCELADA', rotulo: 'Cancelada' },
];

/** Ordem em que a escalação é exibida no detalhe da pelada. */
export const STATUS_DE_PARTICIPACAO: readonly Opcao<StatusParticipacao>[] = [
  { valor: 'CONFIRMADO', rotulo: 'Confirmados' },
  { valor: 'CONVIDADO', rotulo: 'Convidados' },
  { valor: 'LISTA_DE_ESPERA', rotulo: 'Lista de espera' },
  { valor: 'RECUSADO', rotulo: 'Recusados' },
];

/** Mesmo formato exigido pelo `@Pattern` de `UsuarioRequest`. */
export const PADRAO_CELULAR = /^\(?\d{2}\)?\s?9?\d{4}-?\d{4}$/;

/** Cor do selo de status, para a situação da pelada saltar aos olhos na lista. */
export function corDoStatus(status: StatusPelada | undefined): string {
  switch (status) {
    case 'EM_ANDAMENTO':
      return 'bg-amber-100 text-amber-900 dark:bg-amber-900/40 dark:text-amber-200';
    case 'FINALIZADA':
      return 'bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300';
    case 'CANCELADA':
      return 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200';
    default:
      return 'bg-emerald-100 text-emerald-900 dark:bg-emerald-900/40 dark:text-emerald-200';
  }
}

/** `2026-10-15` → `15/10/2026`, sem passar por Date (que desloca por fuso). */
export function dataBr(iso: string | undefined): string {
  if (!iso) {
    return '';
  }
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

/** `19:00:00` → `19:00`. */
export function hora(iso: string | undefined): string {
  return iso ? iso.slice(0, 5) : '';
}
