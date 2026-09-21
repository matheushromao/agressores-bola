import { Component, computed, inject, resource } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { Api, destaques, listarPeladas, rankingGeral } from '../../api';
import { mensagemDeErro } from '../../core/erro';
import { dataBr } from '../../shared/dominio';

@Component({
  selector: 'app-rankings',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './rankings.html',
})
export class Rankings {
  private readonly api = inject(Api);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly dataBr = dataBr;
  protected readonly mensagemDeErro = mensagemDeErro;

  /** Como na listagem de peladas, o filtro vive na URL. */
  private readonly urlParams = toSignal(this.rota.queryParamMap, {
    initialValue: this.rota.snapshot.queryParamMap,
  });

  protected readonly filtros = this.fb.nonNullable.group({
    peladaId: '',
    limite: '10',
  });

  private readonly filtroAtual = computed(() => {
    const p = this.urlParams();
    const peladaId = p.get('peladaId');
    const limite = p.get('limite');
    return {
      peladaId: peladaId ? Number(peladaId) : undefined,
      limite: limite ? Number(limite) : 10,
    };
  });

  /** Para o seletor "restringir a uma pelada". */
  protected readonly peladas = resource({
    params: () => ({ size: 50 }),
    loader: ({ params }) => this.api.invoke(listarPeladas, params),
  });

  protected readonly geral = resource({
    params: () => this.filtroAtual(),
    loader: ({ params }) => this.api.invoke(rankingGeral, params),
  });

  protected readonly destaques = resource({
    params: () => this.filtroAtual(),
    loader: ({ params }) => this.api.invoke(destaques, params),
  });

  /** O nome da pelada filtrada, para o cabeçalho dizer o recorte. */
  protected readonly peladaFiltrada = computed(() => {
    const id = this.filtroAtual().peladaId;
    if (id === undefined) {
      return null;
    }
    return this.peladas.value()?.conteudo?.find((p) => p.id === id) ?? null;
  });

  constructor() {
    this.filtros.patchValue(
      {
        peladaId: this.rota.snapshot.queryParamMap.get('peladaId') ?? '',
        limite: this.rota.snapshot.queryParamMap.get('limite') ?? '10',
      },
      { emitEvent: false },
    );
  }

  protected aplicar(): void {
    const { peladaId, limite } = this.filtros.getRawValue();
    const query: Params = {};
    if (peladaId) {
      query['peladaId'] = peladaId;
    }
    if (limite && limite !== '10') {
      query['limite'] = limite;
    }
    void this.router.navigate([], { relativeTo: this.rota, queryParams: query });
  }
}
