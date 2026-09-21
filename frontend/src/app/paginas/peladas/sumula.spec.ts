import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { EstatisticaResponse, PeladaResponse } from '../../api';
import { provideApiConfiguration } from '../../api/api-configuration';
import { entrarComo, jogador } from '../../testes/fixtures';
import { Sumula } from './sumula';

const ORGANIZADOR = 99;
const JOGADOR = 42;

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
  maxParticipantes: 14,
  totalConfirmados: 1,
  vagasRestantes: 13,
  status: 'FINALIZADA',
  statusDescricao: 'Finalizada',
  organizador: jogador({ id: ORGANIZADOR, nickname: 'ana' }),
  participantes: [
    {
      participacaoId: 10,
      dataInscricao: '2026-09-01T10:00:00',
      status: 'CONFIRMADO',
      statusDescricao: 'Confirmado',
      usuario: jogador({ id: JOGADOR, nickname: 'caio', posicao: 'ALA', posicaoDescricao: 'Ala', estrelas: 3 }),
    },
    {
      participacaoId: 11,
      dataInscricao: '2026-09-01T10:00:00',
      status: 'LISTA_DE_ESPERA',
      statusDescricao: 'Lista de espera',
      usuario: jogador({ id: 77, nickname: 'espera', posicao: 'FIXO', posicaoDescricao: 'Fixo', estrelas: 2 }),
    },
  ],
};

const LANCAMENTOS: EstatisticaResponse[] = [
  {
    id: 500,
    peladaId: 1,
    jogador: jogador({ id: JOGADOR, nickname: 'caio', posicaoDescricao: 'Ala', estrelas: 3 }),
    posicaoJogada: 'ALA',
    posicaoJogadaDescricao: 'Ala',
    goleiro: false,
    gols: 2,
    assistencias: 1,
    desarmes: 3,
    defesas: 0,
    defesasDificeis: 0,
    pontuacao: 36,
    registradaEm: '2026-10-15T21:30:00',
    detalhamento: [
      { atributo: 'GOL', descricao: 'Gols', quantidade: 2, peso: 10, pontos: 20 },
      { atributo: 'ASSISTENCIA', descricao: 'Assistências', quantidade: 1, peso: 7, pontos: 7 },
      { atributo: 'DESARME', descricao: 'Desarmes', quantidade: 3, peso: 3, pontos: 9 },
    ],
  },
];

async function volta(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve));
}

async function abrirSumula(pelada: PeladaResponse = PELADA, lancamentos = LANCAMENTOS) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      provideRouter(
        [
          { path: 'peladas/:id/sumula', component: Sumula },
          { path: 'login', component: LoginFalso },
        ],
        withComponentInputBinding(),
      ),
    ],
  });

  const harness = await RouterTestingHarness.create();
  const httpTesting = TestBed.inject(HttpTestingController);
  await harness.navigateByUrl('/peladas/1/sumula');
  httpTesting.expectOne('/api/peladas/1').flush(pelada);
  httpTesting.expectOne('/api/peladas/1/estatisticas').flush(lancamentos);
  await harness.fixture.whenStable();

  return { harness, httpTesting, texto: () => harness.routeNativeElement?.textContent ?? '' };
}

function botao(harness: RouterTestingHarness, rotulo: string): HTMLButtonElement | undefined {
  return Array.from(harness.routeNativeElement?.querySelectorAll('button') ?? []).find((b) =>
    b.textContent?.includes(rotulo),
  ) as HTMLButtonElement | undefined;
}

function campo(harness: RouterTestingHarness, id: string): HTMLInputElement | null {
  return harness.routeNativeElement?.querySelector<HTMLInputElement>(`#${id}`) ?? null;
}

function escolherPosicao(harness: RouterTestingHarness, valor: string): void {
  const select = harness.routeNativeElement!.querySelector<HTMLSelectElement>('#posicaoJogada')!;
  select.value = valor;
  select.dispatchEvent(new Event('change'));
  harness.detectChanges();
}

describe('Sumula', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('a súmula é leitura pública, sem formulário de lançamento', async () => {
    const { texto, harness, httpTesting } = await abrirSumula();

    expect(texto()).toContain('caio');
    expect(texto()).toContain('36');
    expect(botao(harness, 'Lançar súmula')).toBeUndefined();
    httpTesting.verify();
  });

  it('mostra a quebra de pontos ao clicar no jogador', async () => {
    const { texto, harness, httpTesting } = await abrirSumula();

    botao(harness, 'caio')!.click();
    harness.detectChanges();

    expect(texto()).toContain('De onde vieram os pontos');
    expect(texto()).toContain('Gols: 2 × 10 = 20');
    httpTesting.verify();
  });

  it('pelada que ainda não rolou avisa o organizador em vez de deixar lançar', async () => {
    entrarComo(ORGANIZADOR);
    const { texto, harness, httpTesting } = await abrirSumula({
      ...PELADA,
      status: 'AGENDADA',
      statusDescricao: 'Agendada',
    });

    expect(texto()).toContain('em andamento');
    expect(botao(harness, 'Lançar súmula')).toBeUndefined();
    httpTesting.verify();
  });

  it('só lista confirmados como candidatos à súmula', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSumula();

    const opcoes = Array.from(
      harness.routeNativeElement!.querySelectorAll<HTMLOptionElement>('#jogador option'),
    ).map((o) => o.textContent?.trim());

    expect(opcoes.some((o) => o?.includes('caio'))).toBe(true);
    expect(opcoes.some((o) => o?.includes('espera'))).toBe(false);
    httpTesting.verify();
  });

  /**
   * Defesa é de goleiro, desarme é de linha: a ficha muda com a posição em vez
   * de deixar o organizador lançar um número que a API recusaria com 409.
   */
  it('a ficha troca de campos conforme a posição jogada', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSumula();

    expect(campo(harness, 'desarmes')).not.toBeNull();
    expect(campo(harness, 'defesas')).toBeNull();

    escolherPosicao(harness, 'GOLEIRO');

    expect(campo(harness, 'desarmes')).toBeNull();
    expect(campo(harness, 'defesas')).not.toBeNull();
    expect(campo(harness, 'defesasDificeis')).not.toBeNull();
    httpTesting.verify();
  });

  it('lançar manda PUT com o jogador escolhido e recarrega a súmula', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSumula();

    botao(harness, 'Editar')!.click();
    harness.detectChanges();
    botao(harness, 'Corrigir súmula')!.click();
    await volta();

    const put = httpTesting.expectOne(
      (r) => r.method === 'PUT' && r.url === `/api/peladas/1/participantes/${JOGADOR}/estatistica`,
    );
    expect(put.request.body).toEqual({
      posicaoJogada: 'ALA',
      gols: 2,
      assistencias: 1,
      desarmes: 3,
      defesas: 0,
      defesasDificeis: 0,
    });
    put.flush(LANCAMENTOS[0]);
    await volta();
    harness.detectChanges();
    await volta();

    httpTesting.expectOne('/api/peladas/1/estatisticas').flush(LANCAMENTOS);
    await harness.fixture.whenStable();
    httpTesting.verify();
  });

  it('goleiro não manda desarme, e jogador de linha não manda defesa', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSumula();

    botao(harness, 'Editar')!.click();
    harness.detectChanges();
    escolherPosicao(harness, 'GOLEIRO');
    botao(harness, 'Corrigir súmula')!.click();
    await volta();

    const put = httpTesting.expectOne((r) => r.method === 'PUT');
    expect(put.request.body.posicaoJogada).toBe('GOLEIRO');
    expect(put.request.body.desarmes).toBe(0);
    put.flush(LANCAMENTOS[0]);
    await volta();
    harness.detectChanges();
    await volta();
    httpTesting.expectOne('/api/peladas/1/estatisticas').flush(LANCAMENTOS);
    await harness.fixture.whenStable();
    httpTesting.verify();
  });

  it('apagar manda DELETE do lançamento', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting } = await abrirSumula();

    botao(harness, 'Apagar')!.click();
    await volta();

    const del = httpTesting.expectOne(
      (r) => r.method === 'DELETE' && r.url === `/api/peladas/1/participantes/${JOGADOR}/estatistica`,
    );
    del.flush(null);
    await volta();
    harness.detectChanges();
    await volta();
    httpTesting.expectOne('/api/peladas/1/estatisticas').flush([]);
    await harness.fixture.whenStable();
    httpTesting.verify();
  });

  it('a regra recusada pela API aparece na tela', async () => {
    entrarComo(ORGANIZADOR);
    const { harness, httpTesting, texto } = await abrirSumula();

    botao(harness, 'Editar')!.click();
    harness.detectChanges();
    botao(harness, 'Corrigir súmula')!.click();
    await volta();

    httpTesting
      .expectOne((r) => r.method === 'PUT')
      .flush(
        {
          status: 409,
          erro: 'Regra de negócio violada',
          mensagem: "O atributo 'Defesas' só vale para quem jogou no gol",
        },
        { status: 409, statusText: 'Conflict' },
      );
    await volta();
    harness.detectChanges();

    expect(texto()).toContain('só vale para quem jogou no gol');
    httpTesting.verify();
  });

  it('pelada sem lançamento mostra o estado vazio', async () => {
    const { texto, httpTesting } = await abrirSumula(PELADA, []);

    expect(texto()).toContain('Nenhuma súmula lançada');
    httpTesting.verify();
  });
});
