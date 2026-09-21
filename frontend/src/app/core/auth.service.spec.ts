import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from './auth.service';

const CHAVE = 'agressores.token';

/** Token no formato do `TokenService` do backend: `sub` = id, mais `nickname`. */
function tokenFalso(opcoes: { id: number; nickname: string; expiraEmSegundos: number }): string {
  const carga = {
    sub: String(opcoes.id),
    nickname: opcoes.nickname,
    exp: Math.floor(Date.now() / 1000) + opcoes.expiraEmSegundos,
  };
  const base64url = (valor: string) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(valor)))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  return `${base64url('{"alg":"HS256"}')}.${base64url(JSON.stringify(carga))}.assinatura`;
}

function criarServico(): AuthService {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  return TestBed.inject(AuthService);
}

describe('AuthService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('lê id e nickname do token guardado', () => {
    localStorage.setItem(CHAVE, tokenFalso({ id: 7, nickname: 'hmz', expiraEmSegundos: 3600 }));

    const auth = criarServico();

    expect(auth.autenticado()).toBe(true);
    expect(auth.usuarioId()).toBe(7);
    expect(auth.nickname()).toBe('hmz');
  });

  it('decodifica nickname com acento', () => {
    localStorage.setItem(CHAVE, tokenFalso({ id: 1, nickname: 'romão', expiraEmSegundos: 3600 }));

    expect(criarServico().nickname()).toBe('romão');
  });

  it('descarta token expirado em vez de abrir o app logado', () => {
    localStorage.setItem(CHAVE, tokenFalso({ id: 7, nickname: 'hmz', expiraEmSegundos: -1 }));

    const auth = criarServico();

    expect(auth.autenticado()).toBe(false);
    expect(localStorage.getItem(CHAVE)).toBeNull();
  });

  it('ignora token malformado', () => {
    localStorage.setItem(CHAVE, 'isso-nao-e-um-jwt');

    expect(criarServico().autenticado()).toBe(false);
  });

  it('sair limpa o storage e manda para o login', () => {
    localStorage.setItem(CHAVE, tokenFalso({ id: 7, nickname: 'hmz', expiraEmSegundos: 3600 }));
    const auth = criarServico();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    auth.sair('/peladas/3');

    expect(auth.autenticado()).toBe(false);
    expect(localStorage.getItem(CHAVE)).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { redirect: '/peladas/3' } });
  });
});
