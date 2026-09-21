import { KeyValuePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CadastroRequest } from '../../api';
import { AuthService } from '../../core/auth.service';
import { camposComErro, mensagemDeErro } from '../../core/erro';
import { PADRAO_CELULAR, POSICOES } from '../../shared/dominio';

@Component({
  selector: 'app-cadastro',
  imports: [ReactiveFormsModule, RouterLink, KeyValuePipe],
  templateUrl: './cadastro.html',
})
export class Cadastro {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly posicoes = POSICOES;
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly errosPorCampo = signal<Record<string, string>>({});

  /** Espelha o Bean Validation de `UsuarioRequest` e `CadastroRequest`. */
  protected readonly form = this.fb.nonNullable.group({
    usuario: this.fb.nonNullable.group({
      nomeCompleto: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
      nickname: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      numeroCelular: ['', [Validators.required, Validators.pattern(PADRAO_CELULAR)]],
      idade: [25, [Validators.required, Validators.min(12), Validators.max(100)]],
      posicao: ['ALA' as const, [Validators.required]],
      nacionalidade: ['Brasileiro', [Validators.required, Validators.maxLength(60)]],
      estrelas: [3, [Validators.min(1), Validators.max(5)]],
      descricao: ['', [Validators.maxLength(500)]],
    }),
    senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected async enviar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);
    this.errosPorCampo.set({});

    const { usuario, senha } = this.form.getRawValue();
    const corpo: CadastroRequest = {
      senha,
      usuario: {
        ...usuario,
        // Campos opcionais em branco não devem virar string vazia no banco
        descricao: usuario.descricao.trim() || undefined,
      },
    };

    try {
      await this.auth.cadastrarEEntrar(corpo);
      await this.router.navigateByUrl('/peladas');
    } catch (erro) {
      this.erro.set(mensagemDeErro(erro, 'Não foi possível concluir o cadastro'));
      this.errosPorCampo.set(camposComErro(erro));
    } finally {
      this.enviando.set(false);
    }
  }
}
