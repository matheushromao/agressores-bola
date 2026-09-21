import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideApiConfiguration } from '../../api/api-configuration';
import { PeladasLista } from './peladas-lista';

const PAGINA_VAZIA = {
  conteudo: [],
  pagina: 0,
  tamanho: 10,
  totalElementos: 0,
  totalPaginas: 0,
  primeira: true,
  ultima: true,
};

async function preparar() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      provideRouter([{ path: 'peladas', component: PeladasLista }], withComponentInputBinding()),
    ],
  });
  return {
    harness: await RouterTestingHarness.create(),
    httpTesting: TestBed.inject(HttpTestingController),
  };
}

describe('PeladasLista', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('sem filtros, pede a primeira página', async () => {
    const { harness, httpTesting } = await preparar();

    await harness.navigateByUrl('/peladas');

    const req = httpTesting.expectOne((r) => r.url === '/api/peladas');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.has('cidade')).toBe(false);
    req.flush(PAGINA_VAZIA);
    httpTesting.verify();
  });

  it('a query string da URL vira parâmetro da chamada', async () => {
    const { harness, httpTesting } = await preparar();

    await harness.navigateByUrl(
      '/peladas?cidade=Sorocaba&status=AGENDADA&tipoCampo=FUTSAL&dataInicial=2026-10-01&page=2',
    );

    const req = httpTesting.expectOne((r) => r.url === '/api/peladas');
    expect(req.request.params.get('cidade')).toBe('Sorocaba');
    expect(req.request.params.get('status')).toBe('AGENDADA');
    expect(req.request.params.get('tipoCampo')).toBe('FUTSAL');
    expect(req.request.params.get('dataInicial')).toBe('2026-10-01');
    expect(req.request.params.get('page')).toBe('2');
    req.flush(PAGINA_VAZIA);
    httpTesting.verify();
  });

  it('a URL também reabastece os campos do formulário', async () => {
    const { harness, httpTesting } = await preparar();

    const componente = await harness.navigateByUrl('/peladas?cidade=Sorocaba&status=CANCELADA', PeladasLista);
    httpTesting.expectOne((r) => r.url === '/api/peladas').flush(PAGINA_VAZIA);

    const filtros = (componente as unknown as { filtros: { getRawValue(): Record<string, string> } }).filtros;
    expect(filtros.getRawValue()['cidade']).toBe('Sorocaba');
    expect(filtros.getRawValue()['status']).toBe('CANCELADA');
    httpTesting.verify();
  });

  it('mostra o aviso de lista vazia quando não há resultado', async () => {
    const { harness, httpTesting } = await preparar();

    await harness.navigateByUrl('/peladas');
    httpTesting.expectOne((r) => r.url === '/api/peladas').flush(PAGINA_VAZIA);
    await harness.fixture.whenStable();

    expect(harness.routeNativeElement?.textContent).toContain('Nenhuma pelada encontrada');
    httpTesting.verify();
  });
});
