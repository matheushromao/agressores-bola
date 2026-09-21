import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PeladaResponse } from '../../api';
import { provideApiConfiguration } from '../../api/api-configuration';
import { PeladaFormulario } from './pelada-formulario';

const CHAVE = 'agressores.token';
const ORGANIZADOR = 99;

@Component({ selector: 'app-vazio', template: '' })
class Vazio {}

function entrarComo(id: number): void {
  const carga = { sub: String(id), nickname: 'hmz', exp: Math.floor(Date.now() / 1000) + 3600 };
  const base64url = (valor: string) => btoa(valor).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  localStorage.setItem(CHAVE, `${base64url('{"alg":"HS256"}')}.${base64url(JSON.stringify(carga))}.assinatura`);
}

/** Uma data sempre no futuro, para não depender de quando a suíte roda. */
function daquiUmMes(): string {
  const data = new Date();
  data.setMonth(data.getMonth() + 1);
  return data.toISOString().slice(0, 10);
}

const PELADA: PeladaResponse = {
  id: 1,
  nome: 'Pelada de quinta',
  descricao: 'Levar colete',
  data: daquiUmMes(),
  horaInicio: '19:00:00',
  horaFim: '21:00:00',
  localNome: 'Arena Vila Progresso',
  endereco: 'Rua das Palmeiras, 120',
  cidade: 'Sorocaba',
  estado: 'SP',
  tipoCampo: 'FUTSAL',
  tipoCampoDescricao: 'Futsal',
  status: 'AGENDADA',
  statusDescricao: 'Agendada',
  maxParticipantes: 14,
  totalConfirmados: 6,
  vagasRestantes: 8,
  organizador: { id: ORGANIZADOR, nickname: 'ana' },
  participantes: [],
};

async function volta(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve));
}

async function abrir(url: string, pelada?: PeladaResponse) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      provideRouter(
        [
          { path: 'peladas/nova', component: PeladaFormulario },
          { path: 'peladas/:id/editar', component: PeladaFormulario },
          { path: 'peladas/:id', component: Vazio },
          { path: 'peladas', component: Vazio },
          { path: 'login', component: Vazio },
        ],
        withComponentInputBinding(),
      ),
    ],
  });

  const harness = await RouterTestingHarness.create();
  const httpTesting = TestBed.inject(HttpTestingController);
  await harness.navigateByUrl(url);

  if (pelada) {
    httpTesting.expectOne('/api/peladas/1').flush(pelada);
    await harness.fixture.whenStable();
  }

  return { harness, httpTesting, texto: () => harness.routeNativeElement?.textContent ?? '' };
}

function preencher(harness: RouterTestingHarness, id: string, valor: string): void {
  const campo = harness.routeNativeElement!.querySelector<HTMLInputElement>(`#${id}`)!;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
}

function botao(harness: RouterTestingHarness, rotulo: string): HTMLButtonElement | undefined {
  return Array.from(harness.routeNativeElement?.querySelectorAll('button') ?? []).find((b) =>
    b.textContent?.includes(rotulo),
  ) as HTMLButtonElement | undefined;
}

describe('PeladaFormulario', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('criar não busca pelada nenhuma e manda POST', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrir('/peladas/nova');

    // Sem id na rota, o resource fica ocioso: nada de GET
    httpTesting.expectNone('/api/peladas/1');

    preencher(harness, 'nome', 'Racha de domingo');
    preencher(harness, 'data', daquiUmMes());
    preencher(harness, 'localNome', 'Quadra do Clube');
    preencher(harness, 'endereco', 'Rua Santa Rosa, 45');
    preencher(harness, 'cidade', 'Votorantim');
    harness.detectChanges();

    botao(harness, 'Agendar pelada')!.click();
    await volta();

    const post = httpTesting.expectOne((r) => r.method === 'POST' && r.url === '/api/peladas');
    expect(post.request.body.nome).toBe('Racha de domingo');
    expect(post.request.body.cidade).toBe('Votorantim');
    // A API trabalha com segundos no horário
    expect(post.request.body.horaInicio).toBe('19:00:00');
    expect(post.request.body.horaFim).toBe('21:00:00');
    // O organizador vem do token, nunca do corpo
    expect(post.request.body.organizadorId).toBeUndefined();
    post.flush({ ...PELADA, id: 7 });
    await volta();
    httpTesting.verify();
  });

  it('editar carrega o que está gravado e manda PUT', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrir('/peladas/1/editar', PELADA);

    const nome = harness.routeNativeElement!.querySelector<HTMLInputElement>('#nome')!;
    expect(nome.value).toBe('Pelada de quinta');
    const inicio = harness.routeNativeElement!.querySelector<HTMLInputElement>('#horaInicio')!;
    expect(inicio.value).toBe('19:00');

    preencher(harness, 'nome', 'Pelada de sexta');
    harness.detectChanges();
    botao(harness, 'Salvar alterações')!.click();
    await volta();

    const put = httpTesting.expectOne((r) => r.method === 'PUT' && r.url === '/api/peladas/1');
    expect(put.request.body.nome).toBe('Pelada de sexta');
    put.flush(PELADA);
    await volta();
    httpTesting.verify();
  });

  it('depois de salvar, vai para a pelada', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrir('/peladas/1/editar', PELADA);

    botao(harness, 'Salvar alterações')!.click();
    await volta();
    httpTesting.expectOne((r) => r.method === 'PUT').flush({ ...PELADA, id: 1 });
    await volta();

    expect(TestBed.inject(Router).url).toBe('/peladas/1');
    httpTesting.verify();
  });

  it('quem não organiza não recebe formulário', async () => {
    entrarComo(7);
    const { texto, harness, httpTesting } = await abrir('/peladas/1/editar', PELADA);

    expect(texto()).toContain('Só quem organiza');
    expect(harness.routeNativeElement!.querySelector('#nome')).toBeNull();
    httpTesting.verify();
  });

  it('pelada encerrada avisa e trava o salvamento', async () => {
    entrarComo(ORGANIZADOR);
    const { texto, harness, httpTesting } = await abrir('/peladas/1/editar', {
      ...PELADA,
      status: 'CANCELADA',
      statusDescricao: 'Cancelada',
    });

    expect(texto()).toContain('não aceita mais alterações');
    expect(botao(harness, 'Salvar alterações')!.disabled).toBe(true);
    httpTesting.verify();
  });

  /** O backend exige início no futuro também na edição. */
  it('pelada que já começou pede para remarcar', async () => {
    entrarComo(ORGANIZADOR);
    const { texto, httpTesting } = await abrir('/peladas/1/editar', {
      ...PELADA,
      data: '2020-01-01',
    });

    expect(texto()).toContain('remarque');
    httpTesting.verify();
  });

  it('avisa o limite de confirmados no campo de vagas', async () => {
    entrarComo(ORGANIZADOR);
    const { texto, httpTesting } = await abrir('/peladas/1/editar', PELADA);

    expect(texto()).toContain('menor que os 6 já confirmados');
    httpTesting.verify();
  });

  it('a regra recusada pela API aparece com o campo', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting, texto } = await abrir('/peladas/1/editar', PELADA);

    botao(harness, 'Salvar alterações')!.click();
    await volta();
    httpTesting
      .expectOne((r) => r.method === 'PUT')
      .flush(
        {
          status: 409,
          erro: 'Regra de negócio violada',
          mensagem: 'O organizador já possui uma pelada marcada para esta data e horário',
        },
        { status: 409, statusText: 'Conflict' },
      );
    await volta();
    harness.detectChanges();

    expect(texto()).toContain('já possui uma pelada marcada');
    httpTesting.verify();
  });
});
