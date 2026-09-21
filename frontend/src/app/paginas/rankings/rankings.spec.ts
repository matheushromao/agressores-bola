import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { DestaqueResponse, RankingResponse } from '../../api';
import { provideApiConfiguration } from '../../api/api-configuration';
import { Rankings } from './rankings';

const PELADAS = {
  conteudo: [{ id: 1, nome: 'Pelada de quinta', data: '2026-10-15' }],
  pagina: 0,
  tamanho: 50,
  totalElementos: 1,
  totalPaginas: 1,
  primeira: true,
  ultima: true,
};

const GERAL: RankingResponse[] = [
  {
    posicao: 1,
    jogador: { id: 42, nickname: 'caio', posicaoDescricao: 'Ala', estrelas: 3 },
    jogos: 4,
    gols: 7,
    assistencias: 3,
    desarmes: 5,
    defesas: 0,
    defesasDificeis: 0,
    pontuacao: 106,
    mediaPorJogo: 26.5,
  },
  {
    posicao: 2,
    jogador: { id: 43, nickname: 'bruno', posicaoDescricao: 'Goleiro', estrelas: 4 },
    jogos: 4,
    gols: 0,
    assistencias: 0,
    desarmes: 0,
    defesas: 18,
    defesasDificeis: 5,
    pontuacao: 112,
    mediaPorJogo: 28,
  },
];

const DESTAQUES: DestaqueResponse[] = [
  {
    atributo: 'GOL',
    descricao: 'Gols',
    peso: 10,
    ranking: [{ posicao: 1, jogador: { id: 42, nickname: 'caio' }, jogos: 4, total: 7, pontos: 70 }],
  },
  { atributo: 'DESARME', descricao: 'Desarmes', peso: 3, ranking: [] },
];

function preparar() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      provideRouter([{ path: 'rankings', component: Rankings }]),
    ],
  });
  return { httpTesting: TestBed.inject(HttpTestingController) };
}

async function abrir(url = '/rankings', geral = GERAL, listas = DESTAQUES) {
  const { httpTesting } = preparar();
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);

  const pedidoGeral = httpTesting.expectOne((r) => r.url === '/api/ranking');
  const pedidoDestaques = httpTesting.expectOne((r) => r.url === '/api/ranking/destaques');
  const pedidoPeladas = httpTesting.expectOne((r) => r.url === '/api/peladas');

  pedidoGeral.flush(geral);
  pedidoDestaques.flush(listas);
  pedidoPeladas.flush(PELADAS);
  await harness.fixture.whenStable();

  return {
    harness,
    httpTesting,
    pedidoGeral,
    pedidoDestaques,
    texto: () => harness.routeNativeElement?.textContent ?? '',
  };
}

describe('Rankings', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('sem filtro, soma todas as peladas e usa o limite padrão', async () => {
    const { pedidoGeral, pedidoDestaques, texto, httpTesting } = await abrir();

    expect(pedidoGeral.request.params.has('peladaId')).toBe(false);
    expect(pedidoDestaques.request.params.get('limite')).toBe('10');
    expect(texto()).toContain('Somando todas as peladas');
    httpTesting.verify();
  });

  it('a query string vira o recorte da consulta', async () => {
    const { pedidoGeral, pedidoDestaques, texto, httpTesting } = await abrir(
      '/rankings?peladaId=1&limite=3',
    );

    expect(pedidoGeral.request.params.get('peladaId')).toBe('1');
    expect(pedidoGeral.request.params.get('limite')).toBe('3');
    expect(pedidoDestaques.request.params.get('peladaId')).toBe('1');
    expect(texto()).toContain('Pelada de quinta');
    httpTesting.verify();
  });

  it('desenha a classificação com jogos, média e pontos', async () => {
    const { texto, httpTesting } = await abrir();

    expect(texto()).toContain('caio');
    expect(texto()).toContain('106');
    expect(texto()).toContain('bruno');
    expect(texto()).toContain('28');
    expect(texto()).toContain('1º');
    httpTesting.verify();
  });

  it('desenha os destaques com o peso do atributo', async () => {
    const { texto, httpTesting } = await abrir();

    expect(texto()).toContain('Gols');
    expect(texto()).toContain('10 pts cada');
    expect(texto()).toContain('em 4 jogo(s)');
    expect(texto()).toContain('Ninguém pontuou neste atributo ainda');
    httpTesting.verify();
  });

  it('sem ninguém pontuado, explica que falta lançar súmula', async () => {
    const { texto, httpTesting } = await abrir('/rankings', [], []);

    expect(texto()).toContain('Ninguém pontuou ainda');
    httpTesting.verify();
  });
});
