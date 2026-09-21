import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api, CadastroRequest, LoginRequest, TokenResponse, cadastrar, login } from '../api';

const CHAVE = 'agressores.token';

/**
 * O que o backend coloca no token (ver `TokenService.java`): o `sub` é o id do
 * usuário e é dele que dependem "sou o organizador?" e "já estou escalado?".
 */
export interface Sessao {
  readonly id: number;
  readonly nickname: string;
  readonly expiraEm: Date;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(tokenGuardado());

  readonly token = this._token.asReadonly();
  readonly sessao = computed<Sessao | null>(() => lerSessao(this._token()));
  readonly autenticado = computed(() => this.sessao() !== null);
  readonly usuarioId = computed(() => this.sessao()?.id ?? null);
  readonly nickname = computed(() => this.sessao()?.nickname ?? null);

  async entrar(credenciais: LoginRequest): Promise<void> {
    const resposta = await this.api.invoke(login, { body: credenciais });
    this.guardar(resposta);
  }

  /**
   * Cadastro e login em sequência: quem acabou de se cadastrar não deveria
   * precisar digitar a senha de novo.
   */
  async cadastrarEEntrar(dados: CadastroRequest): Promise<void> {
    await this.api.invoke(cadastrar, { body: dados });
    await this.entrar({ email: dados.usuario.email, senha: dados.senha });
  }

  sair(redirecionarPara?: string): void {
    localStorage.removeItem(CHAVE);
    this._token.set(null);
    void this.router.navigate(['/login'], {
      queryParams: redirecionarPara ? { redirect: redirecionarPara } : {},
    });
  }

  private guardar(resposta: TokenResponse): void {
    const token = resposta.token;
    if (!token || !lerSessao(token)) {
      throw new Error('A API devolveu um token que não pôde ser lido');
    }
    localStorage.setItem(CHAVE, token);
    this._token.set(token);
  }
}

/**
 * Token vencido é descartado antes da primeira chamada — sem isso o app abre
 * "logado" e só descobre o contrário quando a API responde 401.
 */
function tokenGuardado(): string | null {
  const token = seguro(() => localStorage.getItem(CHAVE));
  if (!token) {
    return null;
  }
  if (!lerSessao(token)) {
    seguro(() => localStorage.removeItem(CHAVE));
    return null;
  }
  return token;
}

export function lerSessao(token: string | null): Sessao | null {
  if (!token) {
    return null;
  }

  const carga = decodificarCarga(token);
  if (!carga) {
    return null;
  }

  const id = Number(carga['sub']);
  const exp = Number(carga['exp']);
  if (!Number.isFinite(id) || !Number.isFinite(exp)) {
    return null;
  }

  const expiraEm = new Date(exp * 1000);
  if (expiraEm.getTime() <= Date.now()) {
    return null;
  }

  return { id, nickname: String(carga['nickname'] ?? ''), expiraEm };
}

/**
 * JWT é base64url e o nickname pode ter acento, então não basta `atob`: o
 * resultado precisa passar por uma decodificação UTF-8.
 */
function decodificarCarga(token: string): Record<string, unknown> | null {
  const partes = token.split('.');
  if (partes.length !== 3) {
    return null;
  }

  return seguro(() => {
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const preenchido = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const bytes = Uint8Array.from(atob(preenchido), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes)) as Record<string, unknown>;
  });
}

/** O localStorage lança em aba anônima com dados bloqueados. */
function seguro<T>(acao: () => T): T | null {
  try {
    return acao();
  } catch {
    return null;
  }
}
