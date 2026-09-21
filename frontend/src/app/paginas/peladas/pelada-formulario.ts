import { Component, computed, effect, inject, input, resource, signal } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Api, PeladaRequest, atualizarPelada, buscarPelada, criarPelada } from '../../api';
import { AuthService } from '../../core/auth.service';
import { camposComErro, mensagemDeErro } from '../../core/erro';
import { TIPOS_DE_CAMPO, TipoCampo, dataBr } from '../../shared/dominio';

@Component({
  selector: 'app-pelada-formulario',
  imports: [ReactiveFormsModule, RouterLink, KeyValuePipe],
  templateUrl: './pelada-formulario.html',
})
export class PeladaFormulario {
  /** Presente em `peladas/:id/editar`, ausente em `peladas/nova`. */
  readonly id = input<string>();

  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly tiposDeCampo = TIPOS_DE_CAMPO;
  protected readonly dataBr = dataBr;
  protected readonly mensagemDeErro = mensagemDeErro;

  protected readonly editando = computed(() => this.id() !== undefined);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly errosPorCampo = signal<Record<string, string>>({});

  protected readonly pelada = resource({
    params: () => (this.id() ? { id: Number(this.id()) } : undefined),
    loader: ({ params }) => this.api.invoke(buscarPelada, params),
  });

  protected readonly souOrganizador = computed(() => {
    const dono = this.pelada.value()?.organizador?.id;
    return dono === undefined || dono === this.auth.usuarioId();
  });

  /** Encerrada (finalizada ou cancelada) não aceita mais alteração. */
  protected readonly encerrada = computed(() => {
    const status = this.pelada.value()?.status;
    return status === 'FINALIZADA' || status === 'CANCELADA';
  });

  /**
   * O backend exige início no futuro também na edição, então uma pelada que
   * já começou não pode ser editada sem remarcar data e hora.
   */
  protected readonly inicioNoPassado = computed(() => {
    const jogo = this.pelada.value();
    if (!jogo?.data || !jogo?.horaInicio) {
      return false;
    }
    return new Date(`${jogo.data}T${jogo.horaInicio}`) < new Date();
  });

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    descricao: ['', [Validators.maxLength(500)]],
    data: ['', [Validators.required]],
    horaInicio: ['19:00', [Validators.required]],
    horaFim: ['21:00', [Validators.required]],
    localNome: ['', [Validators.required, Validators.maxLength(120)]],
    endereco: ['', [Validators.required, Validators.maxLength(200)]],
    cidade: ['', [Validators.required, Validators.maxLength(80)]],
    estado: ['SP', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    tipoCampo: 'FUTSAL' as TipoCampo,
    maxParticipantes: [14, [Validators.required, Validators.min(2), Validators.max(50)]],
    valorPorJogador: [0, [Validators.min(0), Validators.max(9999.99)]],
  });

  constructor() {
    // Na edição, o formulário parte do que está gravado
    effect(() => {
      const jogo = this.pelada.value();
      if (!jogo) {
        return;
      }
      this.form.patchValue({
        nome: jogo.nome ?? '',
        descricao: jogo.descricao ?? '',
        data: jogo.data ?? '',
        horaInicio: (jogo.horaInicio ?? '19:00:00').slice(0, 5),
        horaFim: (jogo.horaFim ?? '21:00:00').slice(0, 5),
        localNome: jogo.localNome ?? '',
        endereco: jogo.endereco ?? '',
        cidade: jogo.cidade ?? '',
        estado: jogo.estado ?? 'SP',
        tipoCampo: jogo.tipoCampo ?? 'FUTSAL',
        maxParticipantes: jogo.maxParticipantes ?? 14,
        valorPorJogador: jogo.valorPorJogador ?? 0,
      });
    });
  }

  protected async salvar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();
    const corpo: PeladaRequest = {
      ...valores,
      descricao: valores.descricao.trim() || undefined,
      estado: valores.estado.toUpperCase(),
      // A API trabalha com segundos no horário
      horaInicio: `${valores.horaInicio}:00`,
      horaFim: `${valores.horaFim}:00`,
    };

    this.salvando.set(true);
    this.erro.set(null);
    this.errosPorCampo.set({});
    try {
      const salva = this.editando()
        ? await this.api.invoke(atualizarPelada, { id: Number(this.id()), body: corpo })
        : await this.api.invoke(criarPelada, { body: corpo });
      await this.router.navigate(['/peladas', salva.id]);
    } catch (falha) {
      this.erro.set(mensagemDeErro(falha, 'Não foi possível salvar a pelada'));
      this.errosPorCampo.set(camposComErro(falha));
    } finally {
      this.salvando.set(false);
    }
  }
}
