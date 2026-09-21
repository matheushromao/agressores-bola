import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensagemDeErro } from '../../core/erro';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly rota = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]],
  });

  protected async enviar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);
    try {
      await this.auth.entrar(this.form.getRawValue());
      const destino = this.rota.snapshot.queryParamMap.get('redirect') ?? '/peladas';
      await this.router.navigateByUrl(destino);
    } catch (erro) {
      // O backend não revela qual dos dois está errado, e a tela respeita isso
      this.erro.set(mensagemDeErro(erro, 'E-mail ou senha inválidos'));
    } finally {
      this.enviando.set(false);
    }
  }
}
