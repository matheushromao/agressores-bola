import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PeladaResponse } from '../../api';
import { provideApiConfiguration } from '../../api/api-configuration';
import { entrarComo, jogador } from '../../testes/fixtures';
import { PeladaDetalhe } from './pelada-detalhe';

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
  maxParticipantes: 10,
  totalConfirmados: 1,
  vagasRestantes: 9,
  organizador: jogador({ id: 99, nickname: 'organizador', posicao: 'FIXO', posicaoDescricao: 'Fixo', estrelas: 4 }),
  participantes: [
    {
      participacaoId: 10,
      dataInscricao: '2026-09-01T10:00:00',
      status: 'CONFIRMADO',
      statusDescricao: 'Confirmado',
      usuario: jogador({ id: 42, nickname: 'outro', posicao: 'ALA', posicaoDescricao: 'Ala', estrelas: 3 }),
    },
  ],
};

async function abrirDetalhe() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      provideRouter(
        [
          { path: 'peladas/:id', component: PeladaDetalhe },
          { path: 'login', component: LoginFalso },
        ],
        withComponentInputBinding(),
      ),
    ],
  });

  const harness = await RouterTestingHarness.create();
  const httpTesting = TestBed.inject(HttpTestingController);
  await harness.navigateByUrl('/peladas/1');
  httpTesting.expectOne('/api/peladas/1').flush(PELADA);
  await harness.fixture.whenStable();

  return { harness, httpTesting, texto: () => harness.routeNativeElement?.textContent ?? '' };
}

/**
 * Deixa a fila de tarefas virar. Não dá para usar `whenStable()` aqui: com uma
 * requisição pendente no backend de teste, ele nunca resolve — e o teste
 * estoura o tempo em vez de falhar explicando.
 */
async function volta(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve));
}

function botao(harness: RouterTestingHarness, rotulo: string): HTMLButtonElement | undefined {
  return Array.from(harness.routeNativeElement?.querySelectorAll('button') ?? []).find((b) =>
    b.textContent?.includes(rotulo),
  ) as HTMLButtonElement | undefined;
}

describe('PeladaDetalhe', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('mostra a pelada e a escalação agrupada', async () => {
    const { texto, httpTesting } = await abrirDetalhe();

    expect(texto()).toContain('Pelada de quinta');
    expect(texto()).toContain('15/10/2026');
    expect(texto()).toContain('19:00');
    expect(texto()).toContain('Confirmados');
    expect(texto()).toContain('outro');
    httpTesting.verify();
  });

  it('deslogado, manda para o login em vez de oferecer a ação', async () => {
    const { texto, harness, httpTesting } = await abrirDetalhe();

    expect(texto()).toContain('Entrar para confirmar presença');
    expect(botao(harness, 'Confirmar presença')).toBeUndefined();
    httpTesting.verify();
  });

  it('confirmar presença manda o id do token e recarrega a pelada', async () => {
    entrarComo(7);
    const { harness, httpTesting } = await abrirDetalhe();

    botao(harness, 'Confirmar presença')!.click();
    await volta();

    const post = httpTesting.expectOne(
      (r) => r.method === 'POST' && r.url === '/api/peladas/1/participantes',
    );
    expect(post.request.body).toEqual({ usuarioId: 7, status: 'CONFIRMADO' });
    post.flush({ participacaoId: 11, status: 'CONFIRMADO' });
    await volta();
    // Zoneless: o resource só refaz a chamada quando o template o lê de novo
    harness.detectChanges();
    await volta();

    // A verdade sobre vagas e lista de espera é do backend: a tela relê
    httpTesting.expectOne('/api/peladas/1').flush(PELADA);
    await harness.fixture.whenStable();
    httpTesting.verify();
  });

  it('quem já está escalado vê o próprio status e a saída', async () => {
    entrarComo(42);
    const { texto, harness, httpTesting } = await abrirDetalhe();

    expect(texto()).toContain('Você está nesta pelada');
    botao(harness, 'Sair da pelada')!.click();
    await volta();

    const del = httpTesting.expectOne(
      (r) => r.method === 'DELETE' && r.url === '/api/peladas/1/participantes/42',
    );
    del.flush(null);
    await volta();
    harness.detectChanges();
    await volta();
    httpTesting.expectOne('/api/peladas/1').flush(PELADA);
    await harness.fixture.whenStable();
    httpTesting.verify();
  });

  it('o organizador muda a situação da pelada, que é o que libera súmula e sorteio', async () => {
    entrarComo(99);
    const { harness, httpTesting, texto } = await abrirDetalhe();

    expect(texto()).toContain('Situação da pelada');
    botao(harness, 'Em andamento')!.click();
    await volta();

    const patch = httpTesting.expectOne(
      (r) => r.method === 'PATCH' && r.url === '/api/peladas/1/status',
    );
    expect(patch.request.body).toEqual({ status: 'EM_ANDAMENTO' });
    patch.flush({ ...PELADA, status: 'EM_ANDAMENTO', statusDescricao: 'Em andamento' });
    await volta();
    harness.detectChanges();
    await volta();
    httpTesting.expectOne('/api/peladas/1').flush(PELADA);
    await harness.fixture.whenStable();
    httpTesting.verify();
  });

  it('só o organizador vê os controles sobre os outros jogadores', async () => {
    entrarComo(99);
    const { harness, httpTesting } = await abrirDetalhe();

    expect(botao(harness, 'Recusar')).toBeDefined();
    httpTesting.verify();
  });

  it('erro na ação aparece na tela em vez de sumir', async () => {
    entrarComo(7);
    const { harness, httpTesting, texto } = await abrirDetalhe();

    botao(harness, 'Confirmar presença')!.click();
    await volta();

    httpTesting
      .expectOne((r) => r.method === 'POST')
      .flush(
        { status: 409, erro: 'Regra de negócio violada', mensagem: 'A pelada já está lotada' },
        { status: 409, statusText: 'Conflict' },
      );
    await volta();
    harness.detectChanges();

    expect(texto()).toContain('A pelada já está lotada');
    httpTesting.verify();
  });
});
