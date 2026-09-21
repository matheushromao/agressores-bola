import { Component, computed, inject, input, resource, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Api,
  EstatisticaRequest,
  EstatisticaResponse,
  buscarPelada,
  listarEstatisticas,
  registrarEstatistica,
  removerEstatistica,
} from '../../api';
import { AuthService } from '../../core/auth.service';
import { mensagemDeErro } from '../../core/erro';
import { POSICOES, Posicao, dataBr } from '../../shared/dominio';

@Component({
  selector: 'app-sumula',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './sumula.html',
})
export class Sumula {
  /** Vem da rota `peladas/:id/sumula`. */
  readonly id = input.required<string>();

  private readonly api = inject(Api);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly posicoes = POSICOES;
  protected readonly dataBr = dataBr;
  protected readonly mensagemDeErro = mensagemDeErro;

  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly detalhado = signal<number | null>(null);

  protected readonly pelada = resource({
    params: () => ({ id: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(buscarPelada, params),
  });

  protected readonly sumula = resource({
    params: () => ({ peladaId: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(listarEstatisticas, params),
  });

  protected readonly souOrganizador = computed(
    () => this.auth.usuarioId() !== null && this.pelada.value()?.organizador?.id === this.auth.usuarioId(),
  );

  /** A API só aceita lançamento com a pelada em andamento ou finalizada. */
  protected readonly peladaTeveJogo = computed(() => {
    const status = this.pelada.value()?.status;
    return status === 'EM_ANDAMENTO' || status === 'FINALIZADA';
  });

  /** Só quem confirmou presença pode ter súmula. */
  protected readonly confirmados = computed(
    () => this.pelada.value()?.participantes?.filter((p) => p.status === 'CONFIRMADO') ?? [],
  );

  protected readonly form = this.fb.nonNullable.group({
    usuarioId: [0, [Validators.required, Validators.min(1)]],
    posicaoJogada: 'ALA' as Posicao,
    gols: [0, [Validators.min(0)]],
    assistencias: [0, [Validators.min(0)]],
    desarmes: [0, [Validators.min(0)]],
    defesas: [0, [Validators.min(0)]],
    defesasDificeis: [0, [Validators.min(0)]],
  });

  private readonly valores = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  /**
   * Defesas só contam para quem pegou o gol; desarme é de jogador de linha.
   * A tela mostra a ficha da posição em vez de deixar o organizador lançar um
   * número que a API vai recusar com 409.
   */
  protected readonly goleiro = computed(() => this.valores().posicaoJogada === 'GOLEIRO');

  protected readonly jaLancada = computed(() => {
    const usuarioId = this.valores().usuarioId;
    return this.sumula.value()?.some((e) => e.jogador?.id === usuarioId) ?? false;
  });

  /** Ao trocar de jogador, herda a posição do cadastro — como a API faria. */
  protected selecionarJogador(usuarioId: number): void {
    const participante = this.confirmados().find((p) => p.usuario?.id === usuarioId);
    const lancada = this.sumula.value()?.find((e) => e.jogador?.id === usuarioId);

    this.form.patchValue({
      usuarioId,
      posicaoJogada: lancada?.posicaoJogada ?? participante?.usuario?.posicao ?? 'ALA',
      gols: lancada?.gols ?? 0,
      assistencias: lancada?.assistencias ?? 0,
      desarmes: lancada?.desarmes ?? 0,
      defesas: lancada?.defesas ?? 0,
      defesasDificeis: lancada?.defesasDificeis ?? 0,
    });
  }

  protected editar(estatistica: EstatisticaResponse): void {
    if (estatistica.jogador?.id !== undefined) {
      this.selecionarJogador(estatistica.jogador.id);
      this.erro.set(null);
    }
  }

  protected async salvar(): Promise<void> {
    const valores = this.form.getRawValue();
    if (!valores.usuarioId) {
      this.erro.set('Escolha o jogador da súmula');
      return;
    }

    // Zera o que não vale para a posição, em vez de mandar número que a API recusa
    const ehGoleiro = valores.posicaoJogada === 'GOLEIRO';
    const corpo: EstatisticaRequest = {
      posicaoJogada: valores.posicaoJogada,
      gols: valores.gols,
      assistencias: valores.assistencias,
      desarmes: ehGoleiro ? 0 : valores.desarmes,
      defesas: ehGoleiro ? valores.defesas : 0,
      defesasDificeis: ehGoleiro ? valores.defesasDificeis : 0,
    };

    await this.executar(() =>
      this.api.invoke(registrarEstatistica, {
        peladaId: Number(this.id()),
        usuarioId: valores.usuarioId,
        body: corpo,
      }),
    );
  }

  protected async apagar(usuarioId: number): Promise<void> {
    await this.executar(() =>
      this.api.invoke(removerEstatistica, { peladaId: Number(this.id()), usuarioId }),
    );
  }

  protected alternarDetalhe(id: number | undefined): void {
    this.detalhado.update((atual) => (atual === id ? null : (id ?? null)));
  }

  private async executar(acao: () => Promise<unknown>): Promise<void> {
    this.salvando.set(true);
    this.erro.set(null);
    try {
      await acao();
      // A pontuação e a ordem da súmula são calculadas no backend
      this.sumula.reload();
    } catch (falha) {
      this.erro.set(mensagemDeErro(falha));
    } finally {
      this.salvando.set(false);
    }
  }
}
