import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { MaterialRepository } from '@features/catalogs/materials/domain/repositories/material.repository';
import { HttpMaterialRepository } from '@features/catalogs/materials/infrastructure/repositories/http-material.repository';
import { ProductRepository } from '@features/catalogs/products/domain/repositories/product.repository';
import { HttpProductRepository } from '@features/catalogs/products/infrastructure/repositories/http-product.repository';
import { StructureRepository } from '@features/catalogs/structures/domain/repositories/structure.repository';
import { HttpStructureRepository } from '@features/catalogs/structures/infrastructure/repositories/http-structure.repository';
import { EntryRepository } from '@features/operations/entries/domain/repositories/entry.repository';
import { HttpEntryRepository } from '@features/operations/entries/infrastructure/repositories/http-entry.repository';
import { ExitRepository } from '@features/operations/exits/domain/repositories/exit.repository';
import { HttpExitRepository } from '@features/operations/exits/infrastructure/repositories/http-exit.repository';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    { provide: MaterialRepository, useClass: HttpMaterialRepository },
    { provide: ProductRepository, useClass: HttpProductRepository },
    { provide: StructureRepository, useClass: HttpStructureRepository },
    { provide: EntryRepository, useClass: HttpEntryRepository },
    { provide: ExitRepository, useClass: HttpExitRepository },
  ],
};
