import { Component, computed, inject, input, resource, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  Api,
  ParticipanteResponse,
  adicionarParticipante,
  alterarStatusParticipacao,
  buscarPelada,
  removerParticipante,
} from '../../api';
import { AuthService } from '../../core/auth.service';
import { mensagemDeErro } from '../../core/erro';
import {
  STATUS_DE_PARTICIPACAO,
  StatusParticipacao,
  corDoStatus,
  dataBr,
  hora,
} from '../../shared/dominio';

interface Grupo {
  readonly rotulo: string;
  readonly status: StatusParticipacao;
  readonly jogadores: readonly ParticipanteResponse[];
}

@Component({
  selector: 'app-pelada-detalhe',
  imports: [RouterLink],
  templateUrl: './pelada-detalhe.html',
})
export class PeladaDetalhe {
  /** Vem da rota `peladas/:id` via `withComponentInputBinding()`. */
  readonly id = input.required<string>();

  private readonly api = inject(Api);
  protected readonly auth = inject(AuthService);

  protected readonly corDoStatus = corDoStatus;
  protected readonly dataBr = dataBr;
  protected readonly hora = hora;
  protected readonly mensagemDeErro = mensagemDeErro;

  protected readonly agindo = signal(false);
  protected readonly erroDaAcao = signal<string | null>(null);

  protected readonly pelada = resource({
    params: () => ({ id: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(buscarPelada, params),
  });

  /** Escalação agrupada, na ordem em que interessa ler. */
  protected readonly grupos = computed<Grupo[]>(() => {
    const participantes = this.pelada.value()?.participantes ?? [];
    return STATUS_DE_PARTICIPACAO.map(({ valor, rotulo }) => ({
      rotulo,
      status: valor,
      jogadores: participantes.filter((p) => p.status === valor),
    })).filter((grupo) => grupo.jogadores.length > 0);
  });

  protected readonly souOrganizador = computed(
    () => this.auth.usuarioId() !== null && this.pelada.value()?.organizador?.id === this.auth.usuarioId(),
  );

  /** A participação do usuário logado, se ele já estiver na escalação. */
  protected readonly minhaParticipacao = computed(() => {
    const meuId = this.auth.usuarioId();
    if (meuId === null) {
      return null;
    }
    return this.pelada.value()?.participantes?.find((p) => p.usuario?.id === meuId) ?? null;
  });

  protected async confirmarPresenca(): Promise<void> {
    const meuId = this.auth.usuarioId();
    if (meuId === null) {
      return;
    }
    // Sem vaga, o backend põe na lista de espera sozinho — o front não decide isso
    await this.executar(() =>
      this.api.invoke(adicionarParticipante, {
        id: Number(this.id()),
        body: { usuarioId: meuId, status: 'CONFIRMADO' },
      }),
    );
  }

  protected async sairDaPelada(): Promise<void> {
    const meuId = this.auth.usuarioId();
    if (meuId === null) {
      return;
    }
    await this.executar(() =>
      this.api.invoke(removerParticipante, { id: Number(this.id()), usuarioId: meuId }),
    );
  }

  protected async alterarStatus(usuarioId: number, status: StatusParticipacao): Promise<void> {
    await this.executar(() =>
      this.api.invoke(alterarStatusParticipacao, {
        id: Number(this.id()),
        usuarioId,
        body: { status },
      }),
    );
  }

  /**
   * Toda ação recarrega a pelada: vagas restantes e promoção da lista de
   * espera são calculadas no backend, então refazer a conta aqui só criaria
   * uma segunda verdade.
   */
  private async executar(acao: () => Promise<unknown>): Promise<void> {
    this.agindo.set(true);
    this.erroDaAcao.set(null);
    try {
      await acao();
      this.pelada.reload();
    } catch (erro) {
      this.erroDaAcao.set(mensagemDeErro(erro));
    } finally {
      this.agindo.set(false);
    }
  }
}
