import { Component, computed, inject, input, resource, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, SorteioRequest, SorteioResponse, buscarPelada, sortearTimes } from '../../api';
import { AuthService } from '../../core/auth.service';
import { mensagemDeErro } from '../../core/erro';
import { dataBr, hora } from '../../shared/dominio';

type Criterio = 'times' | 'jogadores';

@Component({
  selector: 'app-sorteio',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './sorteio.html',
})
export class Sorteio {
  /** Vem da rota `peladas/:id/sorteio`. */
  readonly id = input.required<string>();

  private readonly api = inject(Api);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly dataBr = dataBr;
  protected readonly hora = hora;

  protected readonly sorteando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly resultado = signal<SorteioResponse | null>(null);

  protected readonly pelada = resource({
    params: () => ({ id: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(buscarPelada, params),
  });

  protected readonly souOrganizador = computed(
    () => this.auth.usuarioId() !== null && this.pelada.value()?.organizador?.id === this.auth.usuarioId(),
  );

  protected readonly confirmados = computed(() => this.pelada.value()?.totalConfirmados ?? 0);

  protected readonly form = this.fb.nonNullable.group({
    criterio: 'times' as Criterio,
    quantidadeTimes: [2, [Validators.min(2), Validators.max(10)]],
    jogadoresPorTime: [5, [Validators.min(2), Validators.max(11)]],
    semente: '',
  });

  /** O formulário como signal, para a prévia recalcular a cada digitação. */
  private readonly valores = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly criterio = computed<Criterio>(() => this.valores().criterio ?? 'times');

  /**
   * Prévia da divisão antes de chamar a API. O backend é quem decide de
   * verdade — isto existe só para o organizador não descobrir no erro que
   * faltam jogadores.
   */
  protected readonly previa = computed(() => {
    const confirmados = this.confirmados();
    const { quantidadeTimes = 2, jogadoresPorTime = 5 } = this.valores();

    const porCriterioDeTimes = this.criterio() === 'times';
    const times = porCriterioDeTimes ? quantidadeTimes : Math.floor(confirmados / jogadoresPorTime);
    const porTime = porCriterioDeTimes ? Math.floor(confirmados / quantidadeTimes) : jogadoresPorTime;

    if (times < 2 || porTime < 2) {
      return { possivel: false, times, porTime, reservas: 0 };
    }
    return {
      possivel: confirmados >= times * porTime,
      times,
      porTime,
      reservas: confirmados - times * porTime,
    };
  });

  protected async sortear(semente?: number): Promise<void> {
    const valores = this.form.getRawValue();
    const sementeInformada = semente ?? (valores.semente ? Number(valores.semente) : undefined);

    // Exatamente um dos dois critérios: é o que o @AssertTrue do
    // SorteioRequest exige, e mandar os dois dá 400
    const corpo: SorteioRequest =
      valores.criterio === 'times'
        ? { quantidadeTimes: valores.quantidadeTimes, semente: sementeInformada }
        : { jogadoresPorTime: valores.jogadoresPorTime, semente: sementeInformada };

    this.sorteando.set(true);
    this.erro.set(null);
    try {
      const resposta = await this.api.invoke(sortearTimes, { peladaId: Number(this.id()), body: corpo });
      this.resultado.set(resposta);
    } catch (falha) {
      this.resultado.set(null);
      this.erro.set(mensagemDeErro(falha, 'Não foi possível sortear os times'));
    } finally {
      this.sorteando.set(false);
    }
  }

  /** Repete a divisão anterior: a mesma semente produz o mesmo sorteio. */
  protected async repetir(): Promise<void> {
    const semente = this.resultado()?.semente;
    if (semente !== undefined) {
      this.form.controls.semente.setValue(String(semente));
      await this.sortear(semente);
    }
  }

  protected async sortearDeNovo(): Promise<void> {
    this.form.controls.semente.setValue('');
    await this.sortear();
  }

  /** Quanto menor a diferença de estrelas, melhor ficou o equilíbrio. */
  protected readonly qualidade = computed(() => {
    const diferenca = this.resultado()?.diferencaEntreTimes ?? 0;
    if (diferenca <= 0.5) {
      return { rotulo: 'Equilíbrio ótimo', cor: 'text-emerald-700 dark:text-emerald-400' };
    }
    if (diferenca <= 2) {
      return { rotulo: 'Equilíbrio razoável', cor: 'text-amber-700 dark:text-amber-400' };
    }
    return { rotulo: 'Times desequilibrados', cor: 'text-red-700 dark:text-red-400' };
  });
}
