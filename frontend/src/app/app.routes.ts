import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'peladas' },

  {
    path: 'login',
    title: 'Entrar · Agressores da Bola',
    loadComponent: () => import('./paginas/login/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    title: 'Cadastro · Agressores da Bola',
    loadComponent: () => import('./paginas/cadastro/cadastro').then((m) => m.Cadastro),
  },

  // Listagem e detalhe são públicos na API, e aqui também: quem não está
  // logado vê as peladas, só não age sobre elas.
  {
    path: 'peladas',
    title: 'Peladas · Agressores da Bola',
    loadComponent: () => import('./paginas/peladas/peladas-lista').then((m) => m.PeladasLista),
  },
  {
    path: 'peladas/:id',
    title: 'Pelada · Agressores da Bola',
    loadComponent: () => import('./paginas/peladas/pelada-detalhe').then((m) => m.PeladaDetalhe),
  },

  // Pública como a API: a súmula é leitura aberta; lançar exige ser o organizador.
  {
    path: 'peladas/:id/sumula',
    title: 'Súmula · Agressores da Bola',
    loadComponent: () => import('./paginas/peladas/sumula').then((m) => m.Sumula),
  },
  {
    path: 'rankings',
    title: 'Rankings · Agressores da Bola',
    loadComponent: () => import('./paginas/rankings/rankings').then((m) => m.Rankings),
  },

  // Só o organizador sorteia (a API devolve 403), mas o mínimo é estar logado.
  {
    path: 'peladas/:id/sorteio',
    title: 'Sorteio · Agressores da Bola',
    canActivate: [authGuard],
    loadComponent: () => import('./paginas/peladas/sorteio').then((m) => m.Sorteio),
  },

  // Mesma tela da listagem, fixada no organizador do token — daí o guard.
  {
    path: 'minhas-peladas',
    title: 'Minhas peladas · Agressores da Bola',
    canActivate: [authGuard],
    data: { somenteMinhas: true },
    loadComponent: () => import('./paginas/peladas/peladas-lista').then((m) => m.PeladasLista),
  },

  { path: '**', redirectTo: 'peladas' },
];
