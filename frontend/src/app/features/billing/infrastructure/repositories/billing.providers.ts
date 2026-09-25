import { Provider } from '@angular/core';
import { BillingRepository } from '../../domain/repositories/billing.repository';
import { HttpBillingRepository } from './http-billing.repository';

export const BILLING_PROVIDERS: Provider[] = [
  { provide: BillingRepository, useExisting: HttpBillingRepository },
];
