import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PeladaResponse, SorteioResponse } from '../../api';
import { provideApiConfiguration } from '../../api/api-configuration';
import { entrarComo, jogador } from '../../testes/fixtures';
import { Sorteio } from './sorteio';

const ORGANIZADOR = 99;

@Component({ selector: 'app-login-falso', template: '' })
class LoginFalso {}

const PELADA: PeladaResponse = {
  id: 1,
  nome: 'Pelada de quinta',
  data: '2026-10-15',
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
  totalConfirmados: 10,
  vagasRestantes: 4,
  organizador: jogador({ id: ORGANIZADOR, nickname: 'ana', posicao: 'PIVO', posicaoDescricao: 'Pivô', estrelas: 4.5 }),
  participantes: [],
};

const SORTEIO: SorteioResponse = {
  peladaId: 1,
  peladaNome: 'Pelada de quinta',
  quantidadeTimes: 2,
  jogadoresPorTime: 5,
  totalConfirmados: 10,
  diferencaEntreTimes: 0.5,
  semente: 4242,
  times: [
    {
      nome: 'Time A',
      quantidadeJogadores: 5,
      totalEstrelas: 17.5,
      mediaEstrelas: 3.5,
      temGoleiro: true,
      jogadores: [
        { goleiro: true, jogador: jogador({ id: 1, nickname: 'bruno', posicaoDescricao: 'Goleiro', estrelas: 4 }) },
        { goleiro: false, jogador: jogador({ id: 2, nickname: 'caio', posicaoDescricao: 'Ala', estrelas: 3.5 }) },
      ],
    },
    {
      nome: 'Time B',
      quantidadeJogadores: 5,
      totalEstrelas: 17,
      mediaEstrelas: 3.4,
      temGoleiro: false,
      jogadores: [{ goleiro: false, jogador: jogador({ id: 3, nickname: 'davi', posicaoDescricao: 'Fixo', estrelas: 3 }) }],
    },
  ],
  reservas: [{ goleiro: false, jogador: jogador({ id: 9, nickname: 'sobrou', posicaoDescricao: 'Ala', estrelas: 2 }) }],
};

async function volta(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve));
}

async function abrirSorteio(pelada: PeladaResponse = PELADA) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      provideRouter(
        [
          { path: 'peladas/:id/sorteio', component: Sorteio },
          { path: 'login', component: LoginFalso },
        ],
        withComponentInputBinding(),
      ),
    ],
  });

  const harness = await RouterTestingHarness.create();
  const httpTesting = TestBed.inject(HttpTestingController);
  await harness.navigateByUrl('/peladas/1/sorteio');
  httpTesting.expectOne('/api/peladas/1').flush(pelada);
  await harness.fixture.whenStable();

  return { harness, httpTesting, texto: () => harness.routeNativeElement?.textContent ?? '' };
}

function botao(harness: RouterTestingHarness, rotulo: string): HTMLButtonElement | undefined {
  return Array.from(harness.routeNativeElement?.querySelectorAll('button') ?? []).find((b) =>
    b.textContent?.includes(rotulo),
  ) as HTMLButtonElement | undefined;
}

function marcarCriterio(harness: RouterTestingHarness, valor: string): void {
  const radio = harness.routeNativeElement?.querySelector<HTMLInputElement>(`input[value="${valor}"]`);
  radio!.checked = true;
  radio!.dispatchEvent(new Event('change'));
  harness.detectChanges();
}

describe('Sorteio', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('quem não organiza a pelada não vê o formulário', async () => {
    entrarComo(7);
    const { texto, harness, httpTesting } = await abrirSorteio();

    expect(texto()).toContain('Só');
    expect(texto()).toContain('ana');
    expect(botao(harness, 'Sortear times')).toBeUndefined();
    httpTesting.verify();
  });

  it('mostra quantos confirmados entram e a prévia da divisão', async () => {
    entrarComo(ORGANIZADOR);
    const { texto, httpTesting } = await abrirSorteio();

    expect(texto()).toContain('10');
    expect(texto()).toContain('jogadores confirmados');
    expect(texto()).toContain('2 times de 5');
    httpTesting.verify();
  });

  /**
   * O `@AssertTrue` de `SorteioRequest` recusa os dois critérios juntos, então
   * mandar ambos daria 400 — a tela precisa enviar exatamente um.
   */
  it('envia só quantidadeTimes quando o critério é por times', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSorteio();

    botao(harness, 'Sortear times')!.click();
    await volta();

    const req = httpTesting.expectOne((r) => r.method === 'POST' && r.url === '/api/peladas/1/sorteio');
    expect(req.request.body.quantidadeTimes).toBe(2);
    expect(req.request.body.jogadoresPorTime).toBeUndefined();
    req.flush(SORTEIO);
    httpTesting.verify();
  });

  it('envia só jogadoresPorTime quando o critério muda', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSorteio();

    marcarCriterio(harness, 'jogadores');
    botao(harness, 'Sortear times')!.click();
    await volta();

    const req = httpTesting.expectOne((r) => r.method === 'POST');
    expect(req.request.body.jogadoresPorTime).toBe(5);
    expect(req.request.body.quantidadeTimes).toBeUndefined();
    req.flush(SORTEIO);
    httpTesting.verify();
  });

  it('desenha os times, o goleiro, as reservas e a semente', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting, texto } = await abrirSorteio();

    botao(harness, 'Sortear times')!.click();
    await volta();
    httpTesting.expectOne((r) => r.method === 'POST').flush(SORTEIO);
    await volta();
    harness.detectChanges();

    expect(texto()).toContain('Time A');
    expect(texto()).toContain('Time B');
    expect(texto()).toContain('bruno');
    expect(texto()).toContain('17.5');
    expect(texto()).toContain('Sem goleiro'); // o Time B não tem
    expect(texto()).toContain('Reservas');
    expect(texto()).toContain('sobrou');
    expect(texto()).toContain('4242');
    expect(texto()).toContain('Equilíbrio ótimo'); // diferença de 0.5
    httpTesting.verify();
  });

  it('repetir manda a mesma semente do sorteio anterior', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSorteio();

    botao(harness, 'Sortear times')!.click();
    await volta();
    httpTesting.expectOne((r) => r.method === 'POST').flush(SORTEIO);
    await volta();
    harness.detectChanges();

    botao(harness, 'Repetir este sorteio')!.click();
    await volta();

    const repetido = httpTesting.expectOne((r) => r.method === 'POST');
    expect(repetido.request.body.semente).toBe(4242);
    repetido.flush(SORTEIO);
    httpTesting.verify();
  });

  it('a regra de negócio recusada aparece na tela', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting, texto } = await abrirSorteio();

    botao(harness, 'Sortear times')!.click();
    await volta();
    httpTesting
      .expectOne((r) => r.method === 'POST')
      .flush(
        {
          status: 409,
          erro: 'Regra de negócio violada',
          mensagem: 'São necessários 12 jogadores confirmados para formar 2 times de 6, mas a pelada tem 10',
        },
        { status: 409, statusText: 'Conflict' },
      );
    await volta();
    harness.detectChanges();

    expect(texto()).toContain('São necessários 12 jogadores confirmados');
    httpTesting.verify();
  });

  it('com poucos confirmados, avisa e não deixa sortear', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, texto, httpTesting } = await abrirSorteio({ ...PELADA, totalConfirmados: 3 });

    expect(texto()).toContain('não dá para montar essa divisão');
    expect(botao(harness, 'Sortear times')!.disabled).toBe(true);
    httpTesting.verify();
  });
});
