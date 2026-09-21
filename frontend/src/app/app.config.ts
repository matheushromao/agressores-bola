import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideApiConfiguration } from './api/api-configuration';
import { authInterceptor } from './core/auth.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Raiz vazia: as chamadas saem para a própria origem e o proxy de
    // desenvolvimento (proxy.conf.json) as encaminha para o backend na 8080.
    // Em produção, front e API são servidos sob o mesmo domínio.
    provideApiConfiguration(''),
  ],
};
