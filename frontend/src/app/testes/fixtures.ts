import { UsuarioResumoResponse } from '../api';

/**
 * Objetos de teste completos, do jeito que a API entrega.
 *
 * Desde que o contrato passou a declarar `required`, um jogador pela metade
 * não compila — e é isso que se quer: o teste que monta uma resposta
 * incompleta está testando algo que nunca acontece em produção.
 */
export function jogador(mudancas: Partial<UsuarioResumoResponse> = {}): UsuarioResumoResponse {
  return {
    id: 1,
    nickname: 'jogador',
    nomeCompleto: 'Jogador de Teste',
    posicao: 'ALA',
    posicaoDescricao: 'Ala',
    estrelas: 3,
    ...mudancas,
  };
}

const CHAVE = 'agressores.token';

/** Token no formato do `TokenService` do backend: `sub` = id, mais `nickname`. */
export function tokenFalso(
  opcoes: { id?: number; nickname?: string; expiraEmSegundos?: number } = {},
): string {
  const carga = {
    sub: String(opcoes.id ?? 7),
    nickname: opcoes.nickname ?? 'hmz',
    exp: Math.floor(Date.now() / 1000) + (opcoes.expiraEmSegundos ?? 3600),
  };
  return `${base64url('{"alg":"HS256"}')}.${base64url(JSON.stringify(carga))}.assinatura`;
}

export function entrarComo(id: number, nickname = 'hmz'): void {
  localStorage.setItem(CHAVE, tokenFalso({ id, nickname }));
}

/** Base64url com UTF-8: `btoa` sozinho estraga nickname com acento. */
function base64url(valor: string): string {
  return btoa(String.fromCharCode(...new TextEncoder().encode(valor)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}
