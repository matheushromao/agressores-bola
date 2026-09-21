import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

const CHAVE = 'agressores.token';

/** O logout navega para /login; sem a rota o teste morre com NG04002. */
@Component({ selector: 'app-login-falso', template: '' })
class LoginFalso {}

function tokenFalso(expiraEmSegundos = 3600): string {
  const carga = { sub: '7', nickname: 'hmz', exp: Math.floor(Date.now() / 1000) + expiraEmSegundos };
  const base64url = (valor: string) => btoa(valor).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url('{"alg":"HS256"}')}.${base64url(JSON.stringify(carga))}.assinatura`;
}

function preparar() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
      provideRouter([{ path: 'login', component: LoginFalso }]),
    ],
  });
  return {
    http: TestBed.inject(HttpClient),
    httpTesting: TestBed.inject(HttpTestingController),
    auth: TestBed.inject(AuthService),
  };
}

describe('authInterceptor', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('anexa o token quando há sessão', () => {
    localStorage.setItem(CHAVE, tokenFalso());
    const { http, httpTesting } = preparar();

    http.get('/api/usuarios').subscribe();

    const req = httpTesting.expectOne('/api/usuarios');
    expect(req.request.headers.get('Authorization')).toMatch(/^Bearer /);
    req.flush({});
    httpTesting.verify();
  });

  it('não anexa nada quando não há sessão', () => {
    const { http, httpTesting } = preparar();

    http.get('/api/peladas').subscribe();

    const req = httpTesting.expectOne('/api/peladas');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
    httpTesting.verify();
  });

  it('desloga no 401 em vez de deixar a sessão morta de pé', () => {
    localStorage.setItem(CHAVE, tokenFalso());
    const { http, httpTesting, auth } = preparar();
    expect(auth.autenticado()).toBe(true);

    http.get('/api/usuarios').subscribe({ error: () => undefined });
    httpTesting
      .expectOne('/api/usuarios')
      .flush({ status: 401, erro: 'Não autenticado' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.autenticado()).toBe(false);
    expect(localStorage.getItem(CHAVE)).toBeNull();
    httpTesting.verify();
  });

  it('não mexe na sessão em erros que não são 401', () => {
    localStorage.setItem(CHAVE, tokenFalso());
    const { http, httpTesting, auth } = preparar();

    http.get('/api/peladas/9').subscribe({ error: () => undefined });
    httpTesting
      .expectOne('/api/peladas/9')
      .flush({ status: 404 }, { status: 404, statusText: 'Not Found' });

    expect(auth.autenticado()).toBe(true);
    httpTesting.verify();
  });
});
