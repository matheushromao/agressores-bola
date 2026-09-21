import { HttpErrorResponse } from '@angular/common/http';
import { ErroResponse } from '../api';

/**
 * A API tem um contrato único de erro (`ErroResponse`), servido tanto pelo
 * GlobalExceptionHandler quanto pelos 401/403 do filtro de segurança. Traduzir
 * isso num lugar só evita que cada tela invente a própria mensagem.
 */
export function mensagemDeErro(erro: unknown, padrao = 'Não foi possível completar a operação'): string {
  if (!(erro instanceof HttpErrorResponse)) {
    return padrao;
  }

  // status 0: nem chegou na API (backend fora do ar, DNS, CORS)
  if (erro.status === 0) {
    return 'Não foi possível falar com a API. Confira se o backend está no ar.';
  }

  const corpo = erro.error as ErroResponse | null;

  const campos = corpo?.campos;
  if (campos && Object.keys(campos).length > 0) {
    return Object.values(campos).join(' · ');
  }

  return corpo?.mensagem ?? padrao;
}

/**
 * Erros de validação por campo, para o formulário marcar o input certo em vez
 * de jogar tudo num alerta no topo.
 */
export function camposComErro(erro: unknown): Record<string, string> {
  if (erro instanceof HttpErrorResponse) {
    return (erro.error as ErroResponse | null)?.campos ?? {};
  }
  return {};
}
