import {
  MissingTranslationHandler,
  MissingTranslationHandlerParams,
  TranslateService,
} from '@ngx-translate/core';
import { Injectable } from '@angular/core';

@Injectable()
export class VbkMissingTranslationHandler implements MissingTranslationHandler {
  constructor() {}

  public handle(params: MissingTranslationHandlerParams): string {
    if (!this.translationsLoaded(params.translateService)) {
      return '(loading translations)';
    }

    console.log(
      `MissingTranslationHandler(${params.translateService.currentLang}, ${params.key})`
    );

    return `MISSING: ${params.key}`;
  }

  private translationsLoaded(service: TranslateService): boolean {
    return !!service.store.translations[service.currentLang];
  }
}
