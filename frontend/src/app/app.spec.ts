import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

const CHAVE = 'agressores.token';

function tokenFalso(): string {
  const carga = { sub: '7', nickname: 'hmz', exp: Math.floor(Date.now() / 1000) + 3600 };
  const base64url = (valor: string) => btoa(valor).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url('{"alg":"HS256"}')}.${base64url(JSON.stringify(carga))}.assinatura`;
}

async function renderizar(): Promise<HTMLElement> {
  TestBed.configureTestingModule({
    imports: [App],
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(App);
  await fixture.whenStable();
  return fixture.nativeElement as HTMLElement;
}

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('deslogado, oferece entrar e criar conta', async () => {
    const elemento = await renderizar();

    expect(elemento.textContent).toContain('Entrar');
    expect(elemento.textContent).toContain('Criar conta');
    expect(elemento.textContent).not.toContain('Minhas peladas');
  });

  it('logado, mostra o nickname do token e o acesso às minhas peladas', async () => {
    localStorage.setItem(CHAVE, tokenFalso());

    const elemento = await renderizar();

    expect(elemento.textContent).toContain('hmz');
    expect(elemento.textContent).toContain('Minhas peladas');
    expect(elemento.textContent).toContain('Sair');
  });
});
