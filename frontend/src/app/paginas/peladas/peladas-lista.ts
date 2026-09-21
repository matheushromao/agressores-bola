import { Component, effect, inject, resource } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { Api, ListarPeladas$Params, listarPeladas } from '../../api';
import { AuthService } from '../../core/auth.service';
import { mensagemDeErro } from '../../core/erro';
import {
  STATUS_DE_PELADA,
  StatusPelada,
  TIPOS_DE_CAMPO,
  TipoCampo,
  corDoStatus,
  dataBr,
  hora,
} from '../../shared/dominio';

@Component({
  selector: 'app-peladas-lista',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './peladas-lista.html',
})
export class PeladasLista {
  private readonly api = inject(Api);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  protected readonly statusDePelada = STATUS_DE_PELADA;
  protected readonly tiposDeCampo = TIPOS_DE_CAMPO;
  protected readonly corDoStatus = corDoStatus;
  protected readonly dataBr = dataBr;
  protected readonly hora = hora;
  protected readonly mensagemDeErro = mensagemDeErro;

  /** A rota `/minhas-peladas` reaproveita esta tela fixando o organizador. */
  protected readonly somenteMinhas = this.rota.snapshot.data['somenteMinhas'] === true;

  /**
   * A URL é a única fonte de verdade dos filtros: recarregar a página ou
   * compartilhar o link reproduz exatamente a mesma busca.
   */
  private readonly urlParams = toSignal(this.rota.queryParamMap, {
    initialValue: this.rota.snapshot.queryParamMap,
  });

  protected readonly filtros = this.fb.nonNullable.group({
    status: '',
    tipoCampo: '',
    cidade: '',
    dataInicial: '',
    dataFinal: '',
  });

  protected readonly pagina = resource({
    params: (): ListarPeladas$Params => {
      const p = this.urlParams();
      return {
        status: (p.get('status') as StatusPelada | null) ?? undefined,
        tipoCampo: (p.get('tipoCampo') as TipoCampo | null) ?? undefined,
        cidade: p.get('cidade') ?? undefined,
        dataInicial: p.get('dataInicial') ?? undefined,
        dataFinal: p.get('dataFinal') ?? undefined,
        organizadorId: this.somenteMinhas ? (this.auth.usuarioId() ?? undefined) : undefined,
        page: Number(p.get('page') ?? 0),
        size: 10,
      };
    },
    loader: ({ params }) => this.api.invoke(listarPeladas, params),
  });

  constructor() {
    // Voltar pelo histórico também precisa refletir nos campos do formulário
    effect(() => {
      const p = this.urlParams();
      this.filtros.patchValue(
        {
          status: p.get('status') ?? '',
          tipoCampo: p.get('tipoCampo') ?? '',
          cidade: p.get('cidade') ?? '',
          dataInicial: p.get('dataInicial') ?? '',
          dataFinal: p.get('dataFinal') ?? '',
        },
        { emitEvent: false },
      );
    });
  }

  protected aplicar(): void {
    const valores = this.filtros.getRawValue();
    const query: Params = {};
    for (const [campo, valor] of Object.entries(valores)) {
      if (valor) {
        query[campo] = valor;
      }
    }
    // Filtro novo recomeça da primeira página
    void this.router.navigate([], { relativeTo: this.rota, queryParams: query });
  }

  protected limpar(): void {
    this.filtros.reset();
    void this.router.navigate([], { relativeTo: this.rota, queryParams: {} });
  }

  protected irParaPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: { page: pagina },
      queryParamsHandling: 'merge',
    });
  }
}
